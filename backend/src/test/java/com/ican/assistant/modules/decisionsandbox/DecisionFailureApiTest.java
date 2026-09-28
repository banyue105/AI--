package com.ican.assistant.modules.decisionsandbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:decisionfailure;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DecisionFailureApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean DecisionRules rules;

    @Test
    void calculationFailureReturnsActionableErrorWithoutSavingVersion() throws Exception {
        var created = mvc.perform(post("/api/v1/decisions").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"故障场景","goal":"测试失败状态","timeLimitDays":60,"budgetYuan":100000,
                 "peopleCount":3,"hasServer":true,"changeRequest":"增加视觉识别功能"}
                """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).path("id").asText();
        when(rules.simulate(any())).thenThrow(new IllegalStateException("forced failure"));
        mvc.perform(post("/api/v1/decisions/" + id + "/simulate"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("CALCULATION_FAILED"));
    }
}
