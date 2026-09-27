package com.ican.assistant.modules.decisionsandbox;

import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Scenario;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionRulesTest {
    private final DecisionRules rules = new DecisionRules();

    private Scenario scenario(int days, BigDecimal budget, int people, boolean server, String change) {
        return new Scenario("test", "AI 网页", "交付演示", days, budget, people, server, change,
                List.of(), List.of(), List.of(), List.of(), List.of(), 0, 1, Instant.now(), Instant.now());
    }

    @Test
    void visionChangeIncreasesEffortCostAndRiskWithoutInventedPrecision() {
        var results = rules.simulate(scenario(60, new BigDecimal("100000"), 3, true, "增加视觉识别功能"));
        assertThat(results).hasSize(2);
        assertThat(results.get(0).timeRange().min()).isEqualTo(37);
        assertThat(results.get(0).timeRange().max()).isEqualTo(47);
        assertThat(results.get(1).timeRange().min()).isEqualTo(51);
        assertThat(results.get(1).timeRange().max()).isEqualTo(66);
        assertThat(results.get(1).budgetRange().min()).isGreaterThan(results.get(0).budgetRange().min());
        assertThat(results.get(1).riskLevel()).isEqualTo("medium");
        assertThat(results.get(1).assumptions()).anyMatch(text -> text.contains("27–38 人日"));
        assertThat(results.get(1).source()).isEqualTo("rule");
    }

    @Test
    void tighterDeadlineLowerBudgetOrMissingServerRaisesRisk() {
        var normal = rules.simulate(scenario(90, new BigDecimal("120000"), 3, true, "增加视觉识别功能")).get(1);
        var constrained = rules.simulate(scenario(35, new BigDecimal("45000"), 3, false, "增加视觉识别功能")).get(1);
        assertThat(normal.riskLevel()).isEqualTo("low");
        assertThat(constrained.riskLevel()).isEqualTo("high");
        assertThat(constrained.timeRange().min()).isGreaterThan(normal.timeRange().min());
        assertThat(constrained.budgetRange().min()).isGreaterThan(normal.budgetRange().min());
    }
}
