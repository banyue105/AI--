package com.ican.assistant.core.ai;

import com.ican.assistant.modules.abilitygrowth.AbilityDtos.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic keyword adapter. It deliberately makes no LLM requests or mastery claims. */
@Component
public class MockAbilityParser implements AbilityParser {
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
            for (Skill skill : SKILLS) {
                Matcher matcher = skill.keywords().matcher(clause);
                while (matcher.find()) mentions.add(new Mention(skill, matcher.start(), matcher.end()));
            }
            mentions.sort(Comparator.comparingInt(Mention::start));
            for (int i = 0; i < mentions.size(); i++) {
                Mention mention = mentions.get(i);
                String before = clause.substring(0, mention.start());
                int next = i + 1 < mentions.size() ? mentions.get(i + 1).start() : clause.length();
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
