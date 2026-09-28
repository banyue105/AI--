package com.ican.assistant.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.assistant.modules.abilitygrowth.AbilityDtos;
import com.ican.assistant.modules.abilitygrowth.AbilityService;
import com.ican.assistant.modules.abilitygrowth.GrowthPathPlanner;
import com.ican.assistant.modules.knowledge.GenerateKnowledgeRequest;
import com.ican.assistant.core.ai.KnowledgeGenerator;
import com.ican.assistant.modules.knowledge.KnowledgeCatalogService;
import com.ican.assistant.modules.knowledge.KnowledgeTrack;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.ican.assistant.modules.abilitygrowth.AbilityDtos.*;

@Service
public class AiAnalysisService {
    public record ExistingSkill(@NotBlank @Size(max = 64) String id,
                                @NotBlank @Size(max = 120) String name) {}

    public record AbilityUpdateRequest(@NotBlank @Size(max = 10000) String statement) {}
    public record AbilityUpdateSkill(String id, String name, Integer proficiency, String description) {
        public AbilityUpdateSkill(String name, Integer proficiency, String description) { this(null, name, proficiency, description); }
    }
    public record AbilityUpdateRelation(String from, String to, String type, Double confidence) {}
    public record AbilityUpdateResponse(List<AbilityUpdateSkill> skills, List<AbilityUpdateRelation> relations, String message) {}

    public record SkillEnrichmentRequest(@NotBlank @Size(max = 120) String skillName,
                                         @NotNull @Size(max = 1000) List<@Valid ExistingSkill> existingSkills) {}
    public record SkillEnrichmentRelation(String existingSkillId, String type, String reason) {}
    public record SkillEnrichmentQuestion(String question, List<String> options, Integer answerIndex, String explanation) {}
    public record SkillEnrichmentResponse(AbilityUpdateSkill skill,
                                          List<SkillEnrichmentRelation> relations,
                                          List<SkillEnrichmentQuestion> questions) {}

    public record TechStackRequest(@NotBlank @Size(max = 120) String statement) {}
    public record TechStackSkill(String name, Integer proficiency, String description) {}
    public record TechStackStage(String name, String description, List<TechStackSkill> skills) {}
    public record TechStackResponse(String direction, List<TechStackStage> stages) {}

    public record ExplainablePathRequest(@NotBlank @Size(max = 64) String goalId,
                                         @Size(max = 300) String goalTitle,
                                         @Size(max = 1000) List<@NotBlank @Size(max = 64) String> skillIds) {}
    public record ExplainablePathNode(String skillId, String title, String description, String stage) {}
    public record ExplainablePathEdge(String from, String to, String reason) {}
    public record ExplainablePathResponse(List<ExplainablePathNode> nodes,
                                          List<ExplainablePathEdge> edges,
                                          String description) {}

    public record AssessmentQuestion(String id, String prompt, List<String> options,
                                     Integer answerIndex, String explanation) {}
    public record AssessmentResponse(List<AssessmentQuestion> questions) {}

    private final AbilityParser parser;
    private final AbilityService abilityService;
    private final GrowthPathPlanner planner;
    private final KnowledgeGenerator knowledgeGenerator;
    private final KnowledgeCatalogService knowledgeCatalogService;
    private final AiTextClient ai;
    private final ObjectMapper objectMapper;

    @Autowired
    public AiAnalysisService(AbilityParser parser, AbilityService abilityService,
                             GrowthPathPlanner planner, KnowledgeGenerator knowledgeGenerator,
                             KnowledgeCatalogService knowledgeCatalogService, AiTextClient ai, ObjectMapper objectMapper) {
        this.parser = parser;
        this.abilityService = abilityService;
        this.planner = planner;
        this.knowledgeGenerator = knowledgeGenerator;
        this.knowledgeCatalogService = knowledgeCatalogService;
        this.ai = ai;
        this.objectMapper = objectMapper;
    }

    public AiAnalysisService(AbilityParser parser, AbilityService abilityService,
                             GrowthPathPlanner planner, KnowledgeGenerator knowledgeGenerator,
                             AiTextClient ai, ObjectMapper objectMapper) {
        this(parser, abilityService, planner, knowledgeGenerator, null, ai, objectMapper);
    }

    public AbilityUpdateResponse updateAbility(String statement) {
        AbilityGraph graph = abilityService.graph();
        ParseResult result = parser.parse(statement, graph.nodes(), graph.relations());
        List<SkillNode> suggestedNodes = result.suggestedNodes() == null ? List.of() : result.suggestedNodes();
        List<AbilityUpdateSkill> skills = suggestedNodes.stream()
                .filter(node -> node != null && node.id() != null && !node.id().isBlank()
                        && node.name() != null && !node.name().isBlank())
                .map(node -> new AbilityUpdateSkill(node.id(), node.name().strip(),
                        Math.max(0, Math.min(4, Objects.requireNonNullElse(node.level(), 1))),
                        Objects.requireNonNullElse(node.description(), "")))
                .toList();
        var validIds = new java.util.HashSet<String>();
        graph.nodes().forEach(node -> validIds.add(node.id()));
        skills.forEach(skill -> validIds.add(skill.id()));
        List<AbilityUpdateRelation> relations = result.suggestedRelations() == null ? List.of() : result.suggestedRelations().stream()
                .filter(relation -> relation != null && validIds.contains(relation.from()) && validIds.contains(relation.to())
                        && !relation.from().equals(relation.to())
                        && ("prerequisite".equals(relation.type()) || "related".equals(relation.type())))
                .map(relation -> new AbilityUpdateRelation(relation.from(), relation.to(), relation.type(),
                        Math.max(0, Math.min(1, Objects.requireNonNullElse(relation.confidence(), 0.7)))))
                .distinct().toList();
        return new AbilityUpdateResponse(skills, relations, result.summary());
    }

    public SkillEnrichmentResponse enrichSkill(SkillEnrichmentRequest request) {
        if (ai.enabled()) {
            String context = writeJson(Map.of("skillName", request.skillName(), "existingSkills", request.existingSkills()));
            var response = ai.completeJson(
                    "TASK=SKILL_ENRICHMENT。只返回 JSON。分析新技能与现有技能的 prerequisite 或 related 关系，生成简短描述和 3 道选择题。字段 skill:{name,proficiency 0-4,description}、relations:[{existingSkillId,type,reason}]、questions:[{question,options,answerIndex,explanation}]。",
                    context);
            if (response.isPresent()) {
                try {
                    SkillEnrichmentResponse parsed = objectMapper.treeToValue(response.get(), SkillEnrichmentResponse.class);
                    if (parsed != null && parsed.skill() != null) return normalizeEnrichment(parsed, request.skillName());
                } catch (Exception ignored) {
                    // Use a deterministic answer when the model returns malformed JSON.
                }
            }
        }
        return fallbackEnrichment(request.skillName(), request.existingSkills());
    }

    public TechStackResponse generateTechStack(TechStackRequest request) {
        AbilityGraph graph = abilityService.graph();
        List<GenerateKnowledgeRequest.CurrentSkill> current = graph.nodes().stream()
                .map(node -> new GenerateKnowledgeRequest.CurrentSkill(node.name(), node.level(), node.status()))
                .toList();
        var generationRequest = new GenerateKnowledgeRequest(request.statement(), current);
        KnowledgeTrack track = knowledgeCatalogService == null
                ? knowledgeGenerator.generate(generationRequest)
                : knowledgeCatalogService.generate(generationRequest);
        List<TechStackStage> stages = track.stages().stream().map(stage -> new TechStackStage(stage.title(),
                stage.description(), stage.items().stream().map(item -> new TechStackSkill(item.name(),
                        "required".equals(item.priority()) ? 2 : 1, item.description())).toList())).toList();
        return new TechStackResponse(track.title(), stages);
    }

    public ExplainablePathResponse explainablePath(ExplainablePathRequest request) {
        AbilityGraph graph = abilityService.graph();
        List<SkillNode> nodes = request.skillIds() == null || request.skillIds().isEmpty()
                ? graph.nodes() : graph.nodes().stream().filter(node -> request.skillIds().contains(node.id())).toList();
        List<SkillRelation> relations = graph.relations().stream()
                .filter(relation -> nodes.stream().anyMatch(node -> node.id().equals(relation.from()))
                        && nodes.stream().anyMatch(node -> node.id().equals(relation.to())))
                .toList();
        if (ai.enabled()) {
            String goalTitle = request.goalTitle() == null || request.goalTitle().isBlank()
                    ? graph.goal().title() : request.goalTitle().strip();
            String prompt = writeJson(Map.of("goal", goalTitle, "nodes", nodes, "relations", relations));
            var response = ai.completeJson(
                    "TASK=EXPLAINABLE_PATH。只返回 JSON。为目标能力规划有顺序的学习路线。字段 nodes:[{skillId,title,description,stage}]、edges:[{from,to,reason}]、description。只能引用输入中的 skillId，描述具体且可执行。",
                    prompt);
            if (response.isPresent()) {
                try {
                    ExplainablePathResponse parsed = objectMapper.treeToValue(response.get(), ExplainablePathResponse.class);
                    if (parsed != null && parsed.nodes() != null && !parsed.nodes().isEmpty()) {
                        return normalizePath(parsed, nodes, relations);
                    }
                } catch (Exception ignored) {
                    // Fall through to the deterministic planner.
                }
            }
        }
        String goalTitle = request.goalTitle() == null || request.goalTitle().isBlank()
                ? graph.goal().title() : request.goalTitle().strip();
        return fallbackPath(nodes, relations, goalTitle);
    }

    public AssessmentResponse questions(String skillId) {
        AbilityGraph graph = abilityService.graph();
        SkillNode skill = graph.nodes().stream().filter(node -> node.id().equals(skillId)).findFirst().orElse(null);
        if (skill == null) return new AssessmentResponse(List.of());
        if (ai.enabled()) {
            var response = ai.completeJson(
                    "TASK=ASSESSMENT。只返回 JSON，生成 3 道关于指定技能的基础到实践选择题。字段 questions:[{prompt,options,answerIndex,explanation}]，每题 4 个选项，answerIndex 从 0 开始。",
                    writeJson(Map.of("skill", skill)));
            if (response.isPresent()) {
                try {
                    AssessmentResponse parsed = objectMapper.treeToValue(response.get(), AssessmentResponse.class);
                    if (parsed != null && parsed.questions() != null && !parsed.questions().isEmpty()) {
                        return normalizeQuestions(parsed);
                    }
                } catch (Exception ignored) {
                    // Use stable fallback questions.
                }
            }
        }
        return new AssessmentResponse(List.of(
                new AssessmentQuestion(skill.id() + "-1", "以下哪项最能证明你能在真实任务中使用该技能？",
                        List.of("只看过教程", "能独立完成并解释取舍", "收藏了资料", "记住了术语"), 1,
                        "真实任务中的独立应用和复盘，比单纯接触资料更能证明掌握。"),
                new AssessmentQuestion(skill.id() + "-2", "遇到该技能相关问题时，优先做什么？",
                        List.of("直接猜测", "查阅官方资料并做最小验证", "忽略问题", "只看搜索摘要"), 1,
                        "官方资料和可复现验证可以减少错误结论。"),
                new AssessmentQuestion(skill.id() + "-3", "一项技能的实践证据最好包含什么？",
                        List.of("只有标题", "目标、过程、结果和问题", "只保留截图", "只记录耗时"), 1,
                        "完整记录才能支持复盘与后续能力判断。")));
    }

    private SkillEnrichmentResponse fallbackEnrichment(String skillName, List<ExistingSkill> existing) {
        List<SkillEnrichmentRelation> relations = existing.stream()
                .filter(item -> related(skillName, item.name()))
                .map(item -> new SkillEnrichmentRelation(item.id(), "related", "两项技能通常会在同一类任务中协同使用。"))
                .toList();
        return new SkillEnrichmentResponse(
                new AbilityUpdateSkill(skillName.strip(), 1, "围绕“" + skillName.strip() + "”建立概念理解，并通过一个最小实践验证。"),
                relations,
                fallbackQuestions(skillName));
    }

    private List<SkillEnrichmentQuestion> fallbackQuestions(String skillName) {
        return List.of(
                new SkillEnrichmentQuestion("以下哪项最能证明你能在真实任务中使用“" + skillName + "”？", List.of("只看过教程", "能独立完成并解释取舍", "收藏了资料", "记住了术语"), 1, "真实任务中的独立应用和复盘，比单纯接触资料更能证明掌握。"),
                new SkillEnrichmentQuestion("遇到“" + skillName + "”相关问题时，优先做什么？", List.of("直接猜测", "查阅官方资料并做最小验证", "忽略问题", "只看搜索摘要"), 1, "官方资料和可复现验证可以减少错误结论。"),
                new SkillEnrichmentQuestion("一项技能的实践证据最好包含什么？", List.of("只有标题", "目标、过程、结果和问题", "只保留截图", "只记录耗时"), 1, "完整记录才能支持复盘与后续能力判断。"));
    }

    private boolean related(String left, String right) {
        String a = left.toLowerCase(Locale.ROOT);
        String b = right.toLowerCase(Locale.ROOT);
        return a.contains(b) || b.contains(a) || (a.contains("vue") && b.contains("typescript"))
                || (a.contains("spring") && (b.contains("java") || b.contains("mysql")))
                || (a.contains("docker") && (b.contains("linux") || b.contains("部署")));
    }

    private SkillEnrichmentResponse normalizeEnrichment(SkillEnrichmentResponse response, String fallbackName) {
        AbilityUpdateSkill skill = response.skill();
        int proficiency = Math.max(0, Math.min(4, Objects.requireNonNullElse(skill.proficiency(), 1)));
        String name = skill.name() == null || skill.name().isBlank() ? fallbackName : skill.name().strip();
        List<SkillEnrichmentQuestion> questions = response.questions() == null ? List.of() : response.questions().stream()
                .filter(question -> question != null && question.question() != null && question.options() != null && question.options().size() >= 2)
                .limit(5).map(question -> new SkillEnrichmentQuestion(question.question(), question.options(),
                        Math.max(0, Math.min(question.options().size() - 1, Objects.requireNonNullElse(question.answerIndex(), 0))),
                        Objects.requireNonNullElse(question.explanation(), "请结合实践验证答案。"))).toList();
        return new SkillEnrichmentResponse(new AbilityUpdateSkill(null, name, proficiency,
                Objects.requireNonNullElse(skill.description(), "")),
                response.relations() == null ? List.of() : response.relations(), questions);
    }

    private ExplainablePathResponse normalizePath(ExplainablePathResponse response, List<SkillNode> nodes,
                                                  List<SkillRelation> relations) {
        Map<String, SkillNode> byId = nodes.stream().collect(java.util.stream.Collectors.toMap(SkillNode::id, node -> node, (a, b) -> a, LinkedHashMap::new));
        List<ExplainablePathNode> normalizedNodes = response.nodes().stream()
                .filter(node -> node != null && byId.containsKey(node.skillId()))
                .map(node -> new ExplainablePathNode(node.skillId(),
                        node.title() == null || node.title().isBlank() ? byId.get(node.skillId()).name() : node.title(),
                        Objects.requireNonNullElse(node.description(), byId.get(node.skillId()).description()), node.stage()))
                .toList();
        List<ExplainablePathEdge> normalizedEdges = response.edges() == null ? List.of() : response.edges().stream()
                .filter(edge -> edge != null && byId.containsKey(edge.from()) && byId.containsKey(edge.to()))
                .toList();
        return new ExplainablePathResponse(normalizedNodes, normalizedEdges,
                Objects.requireNonNullElse(response.description(), "根据当前能力和前置关系生成的学习路线。"));
    }

    private ExplainablePathResponse fallbackPath(List<SkillNode> nodes, List<SkillRelation> relations, String goalTitle) {
        List<GrowthPathStep> steps = planner.plan(nodes, relations);
        Map<String, SkillNode> byId = nodes.stream().collect(java.util.stream.Collectors.toMap(SkillNode::id, node -> node));
        List<ExplainablePathNode> pathNodes = steps.stream().map(step -> {
            SkillNode node = byId.get(step.skillId());
            return new ExplainablePathNode(step.skillId(), node.name(), node.description(), step.status());
        }).toList();
        List<ExplainablePathEdge> edges = steps.stream().flatMap(step -> step.prerequisiteIds().stream()
                .map(from -> new ExplainablePathEdge(from, step.skillId(), "前置能力关系"))).toList();
        return new ExplainablePathResponse(pathNodes, edges,
                "围绕“" + goalTitle + "”，根据当前能力等级与前置关系生成的可解释学习路线。");
    }

    private AssessmentResponse normalizeQuestions(AssessmentResponse response) {
        List<AssessmentQuestion> questions = response.questions().stream()
                .filter(question -> question != null && question.prompt() != null && question.options() != null && question.options().size() >= 2)
                .limit(5).map(question -> new AssessmentQuestion(
                        question.id() == null || question.id().isBlank() ? UUID.randomUUID().toString() : question.id(),
                        question.prompt(), question.options(), Math.max(0, Math.min(question.options().size() - 1,
                                Objects.requireNonNullElse(question.answerIndex(), 0))),
                        Objects.requireNonNullElse(question.explanation(), "请通过实践验证答案。"))).toList();
        return new AssessmentResponse(questions);
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { return "{}"; }
    }
}
