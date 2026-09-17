package com.ican.assistant.core.ai;

import com.ican.assistant.modules.knowledge.GenerateKnowledgeRequest;
import com.ican.assistant.modules.knowledge.KnowledgeTrack;
import com.ican.assistant.modules.knowledge.KnowledgeTrack.Item;
import com.ican.assistant.modules.knowledge.KnowledgeTrack.Stage;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Deterministic local templates; does not call or impersonate an AI provider. */
@Component
public class MockKnowledgeGenerator implements KnowledgeGenerator {
    private static final Pattern DATA_DIRECTION = Pattern.compile("数据|分析|商业智能|\\bbi\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public KnowledgeTrack generate(GenerateKnowledgeRequest request) {
        String direction = Normalizer.normalize(request.query(), Normalizer.Form.NFKC)
                .strip().replaceAll("\\s+", " ").replaceAll("[?。!]+$", "").strip();
        if (direction.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入有效的知识方向");
        }
        String key = direction.toLowerCase(Locale.ROOT);
        String id = "generated-" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        String title = abbreviate(direction, 24);
        boolean dataDirection = DATA_DIRECTION.matcher(direction).find();
        return new KnowledgeTrack(
                id,
                title,
                abbreviate(title, 8),
                "围绕“" + title + "”整理的本地候选知识结构；真实 AI 模型尚未接入，当前采用固定模板。",
                dataDirection ? "能够独立完成一次从业务问题到分析结论的数据项目"
                        : "能够完成一个可验证的" + title + "实践项目",
                "mock",
                dataDirection ? dataStages(id) : generalStages(id, title));
    }

    private List<Stage> generalStages(String id, String title) {
        return List.of(
                new Stage("custom-foundation", "基础认知", "建立领域概念与问题框架", List.of(
                        item(id, "concept", title + "基础", "核心概念、术语与边界", "required"),
                        item(id, "method", "基础方法", "常见工作方法与判断标准", "required"),
                        item(id, "tool", "核心工具", "完成基础任务所需工具", "recommended"))),
                new Stage("custom-core", "核心能力", "形成可重复的解决问题能力", List.of(
                        item(id, "workflow", "标准流程", "从输入到结果的完整流程", "required"),
                        item(id, "practice", "案例实践", "通过真实案例验证方法", "required"),
                        item(id, "quality", "质量评估", "评价结果质量与风险", "recommended"))),
                new Stage("custom-delivery", "项目交付", "将能力转化为可验证成果", List.of(
                        item(id, "project", "综合项目", "完成端到端实践成果", "required"),
                        item(id, "review", "复盘改进", "记录依据、问题与迭代方向", "required"),
                        item(id, "portfolio", "成果表达", "清晰呈现过程与能力证据", "recommended"))));
    }

    private List<Stage> dataStages(String id) {
        return List.of(
                new Stage("data-foundation", "数据基础", "建立数据处理与统计思维", List.of(
                        item(id, "python", "Python", "数据处理与自动化分析", "required"),
                        item(id, "sql", "SQL", "查询、聚合与数据整理", "required", "MySQL"),
                        item(id, "stats", "统计学", "描述统计、推断与假设检验", "required"))),
                new Stage("data-core", "分析建模", "把问题转换成可验证分析", List.of(
                        item(id, "clean", "数据清洗", "缺失、异常与质量处理", "required", "Pandas"),
                        item(id, "model", "分析建模", "指标体系与基础预测模型", "required", "机器学习"),
                        item(id, "visual", "数据可视化", "图表选择与信息表达", "recommended", "ECharts"))),
                new Stage("data-delivery", "业务交付", "让分析结果可以复用和决策", List.of(
                        item(id, "bi", "BI 看板", "指标监控与交互看板", "required", "商业智能"),
                        item(id, "story", "数据叙事", "结论、依据与限制说明", "required"),
                        item(id, "project", "分析项目", "完成可复现的端到端案例", "recommended"))));
    }

    private Item item(String id, String suffix, String name, String description, String priority, String... aliases) {
        return new Item(id + "-" + suffix, name, description, List.of(aliases), priority);
    }

    private String abbreviate(String text, int maxCodePoints) {
        int length = Math.min(text.codePointCount(0, text.length()), maxCodePoints);
        return text.substring(0, text.offsetByCodePoints(0, length));
    }
}
