package com.ican.assistant.modules.abilitygrowth;

import com.ican.assistant.core.ai.MockAbilityParser;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MockAbilityParserTest {
    private final MockAbilityParser parser = new MockAbilityParser();

    @Test
    void recognizesNegationBeforeAndAfterSkillsAndMakesNoAutomaticWrites() {
        var result = parser.parse("我会 Java 但不会 Docker；Python 不会；我熟练掌握 MySQL，想学习 MyBatis");
        assertThat(result.suggestedNodes()).extracting(AbilityDtos.SkillNode::level).containsExactly(2, 0, 0, 3, 0);
        assertThat(result.suggestedNodes().getLast().status()).isEqualTo("target");
        assertThat(result.assumptions()).anyMatch(value -> value.contains("deterministic mock"));
    }

    @Test
    void producesStableIdsAndDoesNotMisreadJavaScriptAsJava() {
        String text = "我正在学习 JavaScript 和 Spring Boot，了解 Vue3";
        assertThat(parser.parse(text)).isEqualTo(parser.parse(text));
        assertThat(parser.parse(text).suggestedNodes()).extracting(AbilityDtos.SkillNode::name)
                .containsExactly("JavaScript", "Spring Boot", "Vue");
    }

    @Test
    void unknownInputReturnsExplicitEmptyCandidates() {
        assertThat(parser.parse("今天感觉不错").suggestedNodes()).isEmpty();
    }

    @Test
    void negativeMasteryDoesNotBecomePositiveMastery() {
        assertThat(parser.parse("我没有掌握 Java，不太会 Python，没用过 Linux").suggestedNodes())
                .allSatisfy(node -> assertThat(node.level()).isZero());
    }
}
