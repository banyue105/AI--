package com.ican.assistant.core.ai;

import com.ican.assistant.modules.abilitygrowth.AbilityDtos.*;
import com.ican.assistant.modules.knowledge.PresetCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic keyword adapter. It deliberately makes no LLM requests or mastery claims. */
@Component
public class MockAbilityParser implements AbilityParser {
    private final AiTextClient ai;
    private final ObjectMapper objectMapper;
    private final PresetCatalog catalog;

    public MockAbilityParser() { this(null, null, null); }

    @Autowired
    public MockAbilityParser(AiTextClient ai, ObjectMapper objectMapper, PresetCatalog catalog) {
        this.ai = ai;
        this.objectMapper = objectMapper;
        this.catalog = catalog;
    }

    @Override
    public ParseResult parse(String input, List<SkillNode> existingNodes, List<SkillRelation> existingRelations) {
        if (ai != null && ai.enabled() && objectMapper != null) {
            try {
                String context = objectMapper.writeValueAsString(Map.of(
                        "existingNodes", existingNodes == null ? List.of() : existingNodes,
                        "existingRelations", existingRelations == null ? List.of() : existingRelations,
                        "allowedCatalogSkills", allSkills().stream().map(skill -> Map.of("id", skill.id(), "name", skill.name())).toList()));
                var response = ai.completeJson(
                        "你是能力图谱分析器。只返回 JSON，不要 Markdown。只能从 allowedCatalogSkills 中选择技能，或引用 existingNodes 中已有技能；不得新建技能名称、描述或 id。新目录技能 id 必须使用 parsed-加目录 id。等级只能是 0不了解、1了解、2能实践、3熟练、4可指导；status 只能是 mastered、developing、gap、target。关系 from/to 必须引用现有或新节点 id，confidence 为 0 到 1。JSON 字段：summary、suggestedNodes、suggestedRelations、assumptions。",
                        "用户输入：" + input + "\n现有图谱：" + context);
                if (response.isPresent()) {
                    ParseResult result = objectMapper.treeToValue(response.get(), ParseResult.class);
                    if (result != null && result.suggestedNodes() != null) {
                        ParseResult aiResult = sanitizeAiResult(result, existingNodes, existingRelations);
                        ParseResult localResult = inferRelations(parse(input), existingNodes, existingRelations);
                        return mergeResults(aiResult, localResult);
                    }
                }
            } catch (Exception ignored) {
                // Fall through to deterministic extraction when the provider is unavailable or returns invalid JSON.
            }
        }
        return inferRelations(parse(input), existingNodes, existingRelations);
    }
    private record Skill(String id, String name, Pattern keywords) {}
    private record Mention(Skill skill, int start, int end) {}
    private record Assessment(int level, String status) {}
    private static final List<Skill> SKILLS = List.of(
            skill("python", "Python", "python"), skill("linux", "Linux", "linux"),
            skill("tcpip", "TCP/IP", "tcp(?:/ip)?|网络协议"), skill("git", "Git 协作", "git|版本控制"),
            skill("http", "HTTP 服务", "https?|接口设计"), skill("shell", "Shell 自动化", "shell|bash|脚本自动化"),
            skill("docker", "容器化", "docker|容器化"), skill("security", "服务安全", "服务安全|网络安全"),
            skill("monitor", "监控诊断", "监控诊断|日志监控"), skill("deploy", "网络服务部署", "网络服务部署|服务部署"),
            skill("java", "Java", "java"), skill("springboot", "Spring Boot", "spring\\s*boot|springboot"),
            skill("mysql", "MySQL", "mysql"), skill("mybatis", "MyBatis", "mybatis|mybitas"),
            skill("vue", "Vue", "vue(?:\\s*3)?"), skill("typescript", "TypeScript", "typescript"),
            skill("javascript", "JavaScript", "javascript"), skill("kubernetes", "Kubernetes", "kubernetes|k8s"));

    private List<Skill> allSkills() {
        if (catalog == null) return SKILLS;
        Map<String, Skill> merged = new LinkedHashMap<>();
        SKILLS.forEach(skill -> merged.put(skill.id(), skill));
        catalog.tracks().forEach(track -> track.stages().forEach(stage -> stage.items().forEach(item -> {
            String id = item.id();
            String terms = java.util.stream.Stream.concat(java.util.stream.Stream.of(item.name()), item.aliases().stream())
                    .filter(Objects::nonNull).map(String::strip).filter(term -> !term.isBlank())
                    .map(Pattern::quote).collect(java.util.stream.Collectors.joining("|"));
            if (!terms.isBlank()) merged.putIfAbsent(id, skill(id, item.name(), terms));
        })));
        return List.copyOf(merged.values());
    }
    private static final String NEGATIVE = "还不会|不太会|不会|不太熟悉|不熟悉|不熟练|没有掌握|还没掌握|没掌握|不掌握|未掌握|没有学过|没学过|没(?:有)?用过|不太了解|不了解|不懂|还没学|尚未学|从未学|don't\\s+know|do\\s+not\\s+know|not\\s+familiar";
    private static final String TARGET = "想学习|想学|希望学习|目标(?:是)?|计划学习|准备学|想掌握|want\\s+to\\s+learn";
    private static final String MENTOR = "可指导|能指导|指导他人|带教|teach";
    private static final String ADVANCED = "熟练掌握|熟练|擅长|精通|proficient";
    private static final String PRACTICE = "掌握|会使用|能实践|能够使用|使用过|会|know";
    private static final String LEARNING = "正在学习|在学|学习|了解|入门|基础|learning";
    private static final Pattern CUES = Pattern.compile(
            "(" + NEGATIVE + ")|(" + TARGET + ")|(" + MENTOR + ")|(" + ADVANCED + ")|(" + PRACTICE + ")|(" + LEARNING + ")",
            Pattern.CASE_INSENSITIVE);

    @Override
    public ParseResult parse(String input) {
        Map<String, SkillNode> detected = new LinkedHashMap<>();
        for (String clause : input.split("[，,。;；!！?？\\n]+")) {
            List<Mention> mentions = new ArrayList<>();
            for (Skill skill : allSkills()) {
                Matcher matcher = skill.keywords().matcher(clause);
                while (matcher.find()) mentions.add(new Mention(skill, matcher.start(), matcher.end()));
            }
            mentions.sort(Comparator.comparingInt(Mention::start).thenComparing((left, right) -> Integer.compare(right.end(), left.end())));
            int consumedUntil = -1;
            for (int i = 0; i < mentions.size(); i++) {
                Mention mention = mentions.get(i);
                if (mention.start() < consumedUntil) continue;
                String before = clause.substring(0, mention.start());
                int next = clause.length();
                for (int nextIndex = i + 1; nextIndex < mentions.size(); nextIndex++) {
                    if (mentions.get(nextIndex).start() > mention.start()) { next = mentions.get(nextIndex).start(); break; }
                }
                String after = clause.substring(mention.end(), next);
                Assessment assessment = assess(before, after);
                Skill skill = mention.skill();
                int index = detected.size();
                detected.put(skill.id(), new SkillNode("parsed-" + skill.id(), skill.name(),
                        "由本地规则从输入提取，保存前请确认等级与证据。", assessment.level(), assessment.status(),
                        List.of(), 120.0 + index * 190, 360.0));
            }
        }
        return new ParseResult(detected.isEmpty()
                ? "本地规则未识别到明确技能，请补充技能名称或手动新增节点。"
                : "本地规则识别到 " + detected.size() + " 项候选能力，请确认后保存。",
                List.copyOf(detected.values()), List.of(), List.of(
                "来源：deterministic mock（本地关键词规则），未调用大语言模型。",
                "否定表达暂记为不了解（0级）；想学/目标记为目标能力；会/掌握记为能实践（2级）。",
                "仅提及技能且未说明程度时暂记为了解（1级）；熟练为3级，可指导为4级。",
                "规则可能遗漏复杂语义或否定范围；所有候选都需用户确认，提取结果不会自动保存。"));
    }

    private ParseResult mergeResults(ParseResult primary, ParseResult supplemental) {
        Map<String, SkillNode> nodes = new LinkedHashMap<>();
        if (primary.suggestedNodes() != null) primary.suggestedNodes().forEach(node -> nodes.putIfAbsent(normalizedSkillKey(node.name()), node));
        if (supplemental.suggestedNodes() != null) supplemental.suggestedNodes().forEach(node -> nodes.putIfAbsent(normalizedSkillKey(node.name()), node));
        Set<String> nodeIds = nodes.values().stream().map(SkillNode::id).collect(java.util.stream.Collectors.toSet());
        Map<String, SkillRelation> relations = new LinkedHashMap<>();
        java.util.stream.Stream.concat(primary.suggestedRelations() == null ? java.util.stream.Stream.empty() : primary.suggestedRelations().stream(),
                supplemental.suggestedRelations() == null ? java.util.stream.Stream.empty() : supplemental.suggestedRelations().stream())
                .filter(relation -> nodeIds.contains(relation.from()) && nodeIds.contains(relation.to()))
                .forEach(relation -> relations.putIfAbsent(relationKey(relation.from(), relation.to(), relation.type()), relation));
        List<String> assumptions = new ArrayList<>();
        if (primary.assumptions() != null) assumptions.addAll(primary.assumptions());
        if (supplemental.assumptions() != null) supplemental.assumptions().forEach(item -> { if (!assumptions.contains(item)) assumptions.add(item); });
        return new ParseResult(primary.summary(), List.copyOf(nodes.values()), List.copyOf(relations.values()), List.copyOf(assumptions));
    }
    private String normalizedSkillKey(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private record RelationRule(String prerequisite, String dependent) {}

    /** Model output is a candidate only: keep catalog entries and known user nodes, then normalize fields server-side. */
    private ParseResult sanitizeAiResult(ParseResult result, List<SkillNode> existingNodes,
                                         List<SkillRelation> existingRelations) {
        Map<String, SkillNode> known = new LinkedHashMap<>();
        if (existingNodes != null) existingNodes.forEach(node -> known.put(node.id(), node));
        Map<String, Skill> allowed = new LinkedHashMap<>();
        allSkills().forEach(skill -> allowed.put(skill.id(), skill));
        List<SkillNode> nodes = result.suggestedNodes().stream().map(node -> {
            if (node == null) return null;
            Skill existingCatalogSkill = allowed.get(node.id() == null ? "" : node.id().replaceFirst("^parsed-", ""));
            if (existingCatalogSkill != null) return new SkillNode("parsed-" + existingCatalogSkill.id(), existingCatalogSkill.name(),
                    "来自预置技能目录；请在保存前确认熟练程度与证据。", Math.max(0, Math.min(4, node.level())),
                    validStatus(node.status()), List.of(), 0.0, 0.0);
            SkillNode existing = node.id() == null ? null : known.get(node.id());
            return existing == null ? null : new SkillNode(existing.id(), existing.name(), existing.description(),
                    Math.max(0, Math.min(4, node.level())), validStatus(node.status()), existing.evidenceIds(), existing.x(), existing.y());
        }).filter(Objects::nonNull).collect(java.util.stream.Collectors.toMap(SkillNode::id, node -> node, (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();
        return inferRelations(new ParseResult(result.summary() == null ? "已从预置目录匹配候选能力，请确认后保存。" : result.summary(),
                nodes, List.of(), result.assumptions() == null ? List.of() : result.assumptions()), existingNodes, existingRelations);
    }

    private String validStatus(String status) {
        return List.of("mastered", "developing", "gap", "target").contains(status) ? status : "developing";
    }

    private static final List<RelationRule> RELATION_RULES = List.of(
            new RelationRule("linux", "docker"),
            new RelationRule("docker", "kubernetes"),
            new RelationRule("linux", "deploy"),
            new RelationRule("shell", "deploy"),
            new RelationRule("tcpip", "http"),
            new RelationRule("java", "springboot"),
            new RelationRule("java", "mybatis"),
            new RelationRule("mysql", "mybatis"),
            new RelationRule("javascript", "vue"),
            new RelationRule("typescript", "vue"));

    private ParseResult inferRelations(ParseResult result, List<SkillNode> existingNodes,
                                       List<SkillRelation> existingRelations) {
        List<SkillNode> suggested = result.suggestedNodes() == null ? List.of() : result.suggestedNodes();
        if (suggested.size() < 2 && (existingNodes == null || existingNodes.isEmpty())) return result;
        List<SkillNode> known = new ArrayList<>();
        if (existingNodes != null) known.addAll(existingNodes);
        known.addAll(suggested);

        List<SkillRelation> relations = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (existingRelations != null) {
            existingRelations.forEach(item -> seen.add(relationKey(item.from(), item.to(), item.type())));
        }
        for (RelationRule rule : RELATION_RULES) {
            SkillNode prerequisite = findSkill(known, rule.prerequisite());
            SkillNode dependent = findSkill(known, rule.dependent());
            if (prerequisite != null && dependent != null && !sameSkill(prerequisite, dependent)) {
                addRelation(relations, seen, nodeId(prerequisite, existingNodes), nodeId(dependent, existingNodes),
                        "prerequisite", 0.78);
            }
        }
        for (int index = 1; index < suggested.size(); index++) {
            SkillNode from = suggested.get(index - 1);
            SkillNode to = suggested.get(index);
            if (!sameSkill(from, to)) {
                addRelation(relations, seen, nodeId(from, existingNodes), nodeId(to, existingNodes),
                        "related", 0.55);
            }
        }
        if (relations.isEmpty()) return result;
        return new ParseResult(result.summary(), suggested, List.copyOf(relations),
                result.assumptions() == null ? List.of() : result.assumptions());
    }

    private SkillNode findSkill(List<SkillNode> nodes, String canonical) {
        return nodes.stream().filter(node -> matches(node.id(), canonical) || matches(node.name(), canonical))
                .findFirst().orElse(null);
    }

    private boolean matches(String name, String canonical) {
        String value = name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return value.equals(canonical) || value.contains(canonical);
    }

    private boolean sameSkill(SkillNode left, SkillNode right) {
        return left.id().equals(right.id()) || matches(left.name(), right.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""));
    }

    private String nodeId(SkillNode node, List<SkillNode> existingNodes) {
        if (existingNodes != null) {
            return existingNodes.stream()
                    .filter(item -> matches(item.name(), node.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "")))
                    .map(SkillNode::id).findFirst().orElse(node.id());
        }
        return node.id();
    }

    private void addRelation(List<SkillRelation> relations, Set<String> seen, String from, String to,
                             String type, double confidence) {
        if (from == null || to == null || from.equals(to)) return;
        String key = relationKey(from, to, type);
        if (seen.add(key)) relations.add(new SkillRelation(from, to, type, confidence));
    }

    private String relationKey(String from, String to, String type) {
        return from + "|" + to + "|" + type;
    }
    private Assessment assess(String before, String after) {
        Assessment selected = new Assessment(1, "developing");
        Matcher prefix = CUES.matcher(before);
        while (prefix.find()) selected = fromCue(prefix);
        // Covers “Python 不会” without allowing a later clause like “但会 Linux” to overwrite Python.
        Matcher suffix = CUES.matcher(after);
        if (suffix.find() && after.substring(0, suffix.start()).matches("[\\s：:是还尚都也]*")) {
            selected = fromCue(suffix);
        }
        return selected;
    }

    private Assessment fromCue(Matcher cue) {
        if (cue.group(1) != null) return new Assessment(0, "gap");
        if (cue.group(2) != null) return new Assessment(0, "target");
        if (cue.group(3) != null) return new Assessment(4, "mastered");
        if (cue.group(4) != null) return new Assessment(3, "mastered");
        if (cue.group(5) != null) return new Assessment(2, "mastered");
        return new Assessment(1, "developing");
    }

    private static Skill skill(String id, String name, String terms) {
        return new Skill(id, name, Pattern.compile("(?<![a-z0-9])(?:" + terms + ")(?![a-z0-9])", Pattern.CASE_INSENSITIVE));
    }
}
