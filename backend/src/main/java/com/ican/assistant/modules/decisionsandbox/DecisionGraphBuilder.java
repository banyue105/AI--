package com.ican.assistant.modules.decisionsandbox;

import com.ican.assistant.modules.decisionsandbox.DecisionDtos.RelationInput;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.ResourceInput;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.ScenarioInput;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class DecisionGraphBuilder {
    private DecisionGraphBuilder() {}

    static void rebuild(DecisionScenarioEntity scenario, ScenarioInput input) {
        addConstraint(scenario, "time", "时间期限", input.timeLimitDays() + " 天");
        addConstraint(scenario, "budget", "资金预算", input.budgetYuan().stripTrailingZeros().toPlainString() + " 元");
        addConstraint(scenario, "people", "参与人数", input.peopleCount() + " 人");
        addNode(scenario, "goal", "goal", input.goal(), "user", "用户确认的目标");
        addNode(scenario, "time", "constraint", "期限 " + input.timeLimitDays() + " 天", "user", "由场景表单确认");
        addNode(scenario, "budget", "constraint", "预算 " + input.budgetYuan().stripTrailingZeros().toPlainString() + " 元", "user", "由场景表单确认");
        addNode(scenario, "people", "resource", input.peopleCount() + " 名成员", "user", "由场景表单确认");
        if (input.hasServer()) addNode(scenario, "server", "resource", "已有服务器", "user", "由场景表单确认");
        if (!input.changeRequest().isBlank()) addNode(scenario, "feature", "task", input.changeRequest(), "user", "用户确认的功能变更");
        addNode(scenario, "risk", "risk", "交付风险", "system", "由时间、预算与资源的规则比较得出");

        Set<String> resourceIds = new HashSet<>();
        for (ResourceInput resource : input.resources()) {
            if (!Set.of("person", "money", "equipment", "skill", "other").contains(resource.type())) {
                throw new IllegalArgumentException("资源类型不受支持。");
            }
            String id = resource.id() == null || resource.id().isBlank() ? UUID.randomUUID().toString() : resource.id();
            if (!resourceIds.add(id)) throw new IllegalArgumentException("资源标识不能重复。");
            DecisionResourceEntity entity = new DecisionResourceEntity();
            entity.id = scoped(scenario, id);
            entity.scenario = scenario;
            entity.type = resource.type();
            entity.label = resource.label().trim();
            entity.quantity = resource.quantity();
            entity.unit = resource.unit();
            entity.source = "user";
            entity.confidence = BigDecimal.ONE;
            entity.assumption = "由用户在场景表单中确认";
            scenario.resources.add(entity);
            addNode(scenario, "resource-" + id, "resource", entity.label, "user", entity.assumption);
        }

        Map<String, RelationInput> overrides = new HashMap<>();
        for (RelationInput relation : input.relations()) {
            if (relation.confidence().compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("关系置信度必须在 0–1 之间。");
            overrides.put(relation.id() == null || relation.id().isBlank() ? relation.from() + "-" + relation.to() : relation.id(), relation);
        }
        addRelation(scenario, overrides, "people-goal", "people", "goal", "影响实施速度");
        addRelation(scenario, overrides, "budget-goal", "budget", "goal", "约束资源投入");
        addRelation(scenario, overrides, "time-goal", "time", "goal", "限制交付窗口");
        if (input.hasServer()) addRelation(scenario, overrides, "server-goal", "server", "goal", "复用部署环境");
        if (!input.changeRequest().isBlank()) {
            addRelation(scenario, overrides, "feature-goal", "feature", "goal", "增加工作量");
            addRelation(scenario, overrides, "feature-risk", "feature", "risk", "增加验证风险");
        }
        addRelation(scenario, overrides, "risk-goal", "risk", "goal", "影响交付判断");
        for (String id : resourceIds) addRelation(scenario, overrides, "resource-" + id + "-goal", "resource-" + id, "goal", "提供实施资源");
    }

    private static void addConstraint(DecisionScenarioEntity scenario, String type, String label, String value) {
        DecisionConstraintEntity item = new DecisionConstraintEntity();
        item.id = scoped(scenario, type);
        item.scenario = scenario;
        item.type = type;
        item.label = label;
        item.value = value;
        item.source = "user";
        item.confidence = BigDecimal.ONE;
        item.assumption = "由用户在场景表单中确认";
        scenario.constraints.add(item);
    }

    private static void addNode(DecisionScenarioEntity scenario, String id, String type, String label, String source, String assumption) {
        DecisionNodeEntity node = new DecisionNodeEntity();
        node.id = scoped(scenario, id);
        node.scenario = scenario;
        node.type = type;
        node.label = label;
        node.source = source;
        node.confidence = BigDecimal.ONE;
        node.assumption = assumption;
        scenario.nodes.add(node);
    }

    private static void addRelation(DecisionScenarioEntity scenario, Map<String, RelationInput> overrides,
                                    String id, String from, String to, String defaultLabel) {
        RelationInput override = overrides.get(id);
        DecisionRelationEntity relation = new DecisionRelationEntity();
        relation.id = scoped(scenario, id);
        relation.scenario = scenario;
        relation.fromNodeId = from;
        relation.toNodeId = to;
        relation.label = override == null ? defaultLabel : override.label().trim();
        relation.source = override == null ? "system" : "user";
        relation.confidence = override == null ? new BigDecimal("0.85") : override.confidence();
        relation.assumption = override == null ? "由条件关系规则生成，可由用户修正" : override.assumption();
        scenario.relations.add(relation);
    }

    static String scoped(DecisionScenarioEntity scenario, String logicalId) {
        return scenario.id + ":" + logicalId;
    }

    static String logical(DecisionScenarioEntity scenario, String storedId) {
        return storedId.substring(scenario.id.length() + 1);
    }
}
