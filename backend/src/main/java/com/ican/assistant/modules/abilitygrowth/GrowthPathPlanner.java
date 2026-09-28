package com.ican.assistant.modules.abilitygrowth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.ican.assistant.modules.abilitygrowth.AbilityDtos.*;

/** Plans from the submitted snapshot; does not change persisted skills or infer study durations. */
@Component
public class GrowthPathPlanner {
    public List<GrowthPathStep> plan(List<SkillNode> nodes, List<SkillRelation> relations) {
        Map<String, SkillNode> byId = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> prerequisites = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> dependents = new LinkedHashMap<>();
        for (SkillNode node : nodes) {
            if (byId.putIfAbsent(node.id(), node) != null) {
                throw badRequest("能力 id 重复：" + node.id());
            }
            prerequisites.put(node.id(), new LinkedHashSet<>());
            dependents.put(node.id(), new LinkedHashSet<>());
        }
        for (SkillRelation relation : relations) {
            if (!byId.containsKey(relation.from()) || !byId.containsKey(relation.to())) {
                throw badRequest("关系必须连接图谱中存在的两个能力");
            }
            if (relation.from().equals(relation.to())) {
                throw badRequest("能力不能关联自身");
            }
            if ("prerequisite".equals(relation.type())) {
                prerequisites.get(relation.to()).add(relation.from());
                dependents.get(relation.from()).add(relation.to());
            } else if (!"related".equals(relation.type())) {
                throw badRequest("不支持的关系类型");
            }
        }
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Deque<String> ready = new ArrayDeque<>();
        prerequisites.forEach((id, required) -> {
            inDegree.put(id, required.size());
            if (required.isEmpty()) ready.addLast(id);
        });
        List<String> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.removeFirst();
            ordered.add(id);
            for (String dependent : dependents.get(id)) {
                if (inDegree.compute(dependent, (key, value) -> value - 1) == 0) ready.addLast(dependent);
            }
        }
        if (ordered.size() != nodes.size()) throw badRequest("前置关系存在循环，请移除循环依赖后重试");

        List<GrowthPathStep> result = new ArrayList<>();
        for (String id : ordered) {
            SkillNode node = byId.get(id);
            List<String> required = List.copyOf(prerequisites.get(id));
            List<String> missing = required.stream().filter(key -> byId.get(key).level() < 2)
                    .map(key -> byId.get(key).name()).toList();
            String status = node.level() >= 2 ? "done" : missing.isEmpty() ? "next" : "blocked";
            String reason = switch (status) {
                case "done" -> "依据当前填写等级，已达到能实践（2级）；建议用实践证据验证。";
                case "blocked" -> "前置能力尚未达到能实践（2级）：" + String.join("、", missing) + "。";
                default -> required.isEmpty() ? "没有未满足的前置依赖，可从此能力开始实践。"
                        : "前置能力均已达到能实践（2级），可继续补齐此能力。";
            };
            result.add(new GrowthPathStep(id, reason, required, status));
        }
        return List.copyOf(result);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
