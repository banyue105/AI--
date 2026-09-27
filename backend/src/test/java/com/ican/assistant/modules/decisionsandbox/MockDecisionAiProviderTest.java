package com.ican.assistant.modules.decisionsandbox;

import com.ican.assistant.core.ai.MockDecisionAiProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockDecisionAiProviderTest {
    private final MockDecisionAiProvider parser = new MockDecisionAiProvider();

    @Test
    void parsesNegativeServerConditionBeforePositiveSubstring() {
        var result = parser.parse("3 人、10 万元、60 天、没有服务器，增加视觉识别功能");
        assertThat(result.candidates()).hasSize(5);
        assertThat(result.candidates().stream().filter(candidate -> candidate.kind().equals("server"))
                .findFirst().orElseThrow().value()).isEqualTo("false");
    }

    @Test
    void unknownTextLeavesManualCorrectionAvailable() {
        var result = parser.parse("帮我想想下一步");
        assertThat(result.candidates()).isEmpty();
        assertThat(result.summary()).contains("未识别");
    }
}
