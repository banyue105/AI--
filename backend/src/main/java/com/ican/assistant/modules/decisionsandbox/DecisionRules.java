package com.ican.assistant.modules.decisionsandbox;

import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Range;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Scenario;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.SimulationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Transparent planning assumptions, not a forecast of actual delivery. */
@Service
public class DecisionRules {
    private static final int BASE_EFFORT_MIN = 72;
    private static final int BASE_EFFORT_MAX = 90;
    private static final int PERSON_DAY_COST_YUAN = 600;

    public List<SimulationResult> simulate(Scenario scenario) {
        boolean hasChange = scenario.changeRequest() != null && !scenario.changeRequest().isBlank();
        int extraMin = hasChange ? (isVisionChange(scenario.changeRequest()) ? 27 : 18) : 0;
        int extraMax = hasChange ? (isVisionChange(scenario.changeRequest()) ? 38 : 30) : 0;
        SimulationResult baseline = calculate(scenario, "baseline", "基准方案", 0, 0);
        SimulationResult changed = calculate(scenario, "changed", "变更方案", extraMin, extraMax);
        return List.of(baseline, changed);
    }

    private SimulationResult calculate(Scenario scenario, String key, String name, int extraMin, int extraMax) {
        int infrastructureDays = scenario.hasServer() ? 0 : 8;
        int infrastructureCost = scenario.hasServer() ? 0 : 8000;
        double effectiveDailyCapacity = Math.max(1, scenario.peopleCount()) * 0.65;
        int minDays = (int) Math.ceil((BASE_EFFORT_MIN + extraMin) / effectiveDailyCapacity) + infrastructureDays;
        int maxDays = (int) Math.ceil((BASE_EFFORT_MAX + extraMax) / effectiveDailyCapacity) + infrastructureDays;
        int minBudget = roundUpThousand((BASE_EFFORT_MIN + extraMin) * PERSON_DAY_COST_YUAN + infrastructureCost);
        int maxBudget = roundUpThousand((BASE_EFFORT_MAX + extraMax) * PERSON_DAY_COST_YUAN + infrastructureCost + 5000);
        int peopleMax = scenario.peopleCount() + (extraMin > 0 ? 1 : 0);
        int deadline = scenario.timeLimitDays();
        double budget = scenario.budgetYuan().doubleValue();
        String risk = maxDays > deadline * 1.25 || maxBudget > budget * 1.2 ? "high"
                : maxDays > deadline || maxBudget > budget || !scenario.hasServer() ? "medium" : "low";

        List<String> assumptions = new ArrayList<>(List.of(
                "基准工作量参考 72–90 人日；并非对具体项目的承诺。",
                "每名成员按 65% 有效产能、人日成本 600 元估算。",
                "成本上限包含 5000 元测试与意外开支预留。"));
        if (extraMin > 0) assumptions.add("本次变更额外工作量按 " + extraMin + "–" + extraMax + " 人日估算，需通过实际任务校准。");
        if (!scenario.hasServer()) assumptions.add("缺少可复用服务器，额外计入 8 天准备期和 8000 元环境费用。");

        List<String> resources = new ArrayList<>();
        resources.add(scenario.hasServer() ? "复用现有服务器" : "新增服务器或云环境");
        scenario.resources().forEach(resource -> resources.add(resource.label()));
        if (extraMin > 0) resources.add(isVisionChange(scenario.changeRequest()) ? "视觉样本与模型评估环境" : "变更功能的测试与验证资源");

        List<String> impacts = new ArrayList<>();
        impacts.add(key.equals("baseline") ? "保持当前范围，作为变更比较的参照。" : "“" + scenario.changeRequest() + "”增加开发、集成与验证工作。");
        if (maxDays > deadline) impacts.add("工期区间上限超过当前 " + deadline + " 天期限，需要缩小范围或调整资源。");
        if (maxBudget > budget) impacts.add("成本区间上限超过当前预算，需要确认资金余量。");
        if (peopleMax > scenario.peopleCount()) impacts.add("高负载阶段可能需要增加 1 名临时协作人员，或延长时间。");

        return new SimulationResult(UUID.randomUUID().toString(), scenario.id(), key, name,
                new Range(minDays, maxDays, "day"), new Range(minBudget, maxBudget, "yuan"),
                new Range(scenario.peopleCount(), peopleMax, "person"), resources, risk,
                assumptions, impacts, "rule", "", "mock");
    }

    private boolean isVisionChange(String change) {
        return change.contains("视觉") || change.contains("图像") || change.contains("识别") || change.toLowerCase().contains("vision");
    }

    private int roundUpThousand(int amount) {
        return (int) (Math.ceil(amount / 1000.0) * 1000);
    }
}
