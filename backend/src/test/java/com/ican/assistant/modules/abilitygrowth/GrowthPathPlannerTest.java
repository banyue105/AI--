package com.ican.assistant.modules.abilitygrowth;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static com.ican.assistant.modules.abilitygrowth.AbilityDtos.*;
import static org.assertj.core.api.Assertions.*;

class GrowthPathPlannerTest {
    private final GrowthPathPlanner planner = new GrowthPathPlanner();

    @Test
    void ordersPreviouslyUnknownSkillsAndAppliesLevelTwoThreshold() {
        var nodes = List.of(node("target", 0), node("new-practice", 1), node("new-foundation", 2));
        var relations = List.of(relation("new-foundation", "new-practice", "prerequisite"),
                relation("new-practice", "target", "prerequisite"));
        var result = planner.plan(nodes, relations);
        assertThat(result).extracting(GrowthPathStep::skillId).containsExactly("new-foundation", "new-practice", "target");
        assertThat(result).extracting(GrowthPathStep::status).containsExactly("done", "next", "blocked");
        assertThat(result.getLast().reason()).contains("new-practice", "2级");
    }

    @Test
    void relatedEdgesNeitherBlockNorIntroduceFalseCycles() {
        var result = planner.plan(List.of(node("a", 0), node("b", 0)),
                List.of(relation("a", "b", "related"), relation("b", "a", "related")));
        assertThat(result).allSatisfy(step -> {
            assertThat(step.status()).isEqualTo("next");
            assertThat(step.prerequisiteIds()).isEmpty();
        });
    }

    @Test
    void detectsPrerequisiteCyclesDuplicateIdsAndDanglingReferences() {
        assertThatThrownBy(() -> planner.plan(List.of(node("a", 0), node("b", 0)),
                List.of(relation("a", "b", "prerequisite"), relation("b", "a", "prerequisite"))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("循环");
        assertThatThrownBy(() -> planner.plan(List.of(node("a", 0), node("a", 1)), List.of()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("重复");
        assertThatThrownBy(() -> planner.plan(List.of(node("a", 0)), List.of(relation("missing", "a", "related"))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("存在");
    }

    @Test
    void emptyGraphHasNoSteps() {
        assertThat(planner.plan(List.of(), List.of())).isEmpty();
    }

    private static SkillNode node(String id, int level) {
        return new SkillNode(id, id, "", level, "developing", List.of(), 0.0, 0.0);
    }
    private static SkillRelation relation(String from, String to, String type) {
        return new SkillRelation(from, to, type, 1.0);
    }
}
