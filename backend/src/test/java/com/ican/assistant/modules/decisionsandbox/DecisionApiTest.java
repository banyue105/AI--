package com.ican.assistant.modules.decisionsandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DecisionApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String input(int days, int budget, int people, String change) {
        return """
                {"title":"60 天完成 AI 网页项目","goal":"交付演示项目","timeLimitDays":%d,
                 "budgetYuan":%d,"peopleCount":%d,"hasServer":true,"changeRequest":"%s",
                 "resources":[{"label":"视觉开发","type":"skill","quantity":1,"unit":"人"}]}
                """.formatted(days, budget, people, change);
    }

    private JsonNode json(String body) throws Exception { return mapper.readTree(body); }

    @Test
    void completeAcceptanceFlowPersistsVersionsAndAllowsRestore() throws Exception {
        JsonNode created = json(mvc.perform(post("/api/v1/decisions").contentType(MediaType.APPLICATION_JSON)
                .content(input(60, 100000, 3, ""))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String id = created.path("id").asText();
        assertThat(created.path("nodes").size()).isGreaterThan(4);

        JsonNode parsed = json(mvc.perform(post("/api/v1/decisions/" + id + "/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":\"3 人、10 万元、60 天、已有服务器，增加视觉识别功能\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(parsed.path("source").asText()).isEqualTo("mock");
        assertThat(parsed.path("candidates").size()).isEqualTo(5);

        mvc.perform(put("/api/v1/decisions/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(input(60, 100000, 3, "增加视觉识别功能"))).andExpect(status().isOk());
        JsonNode first = json(mvc.perform(post("/api/v1/decisions/" + id + "/simulate"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(first.path("results").size()).isEqualTo(2);
        assertThat(first.path("results").get(1).path("timeRange").path("max").asInt()).isEqualTo(66);
        assertThat(first.path("results").get(1).path("explanationSource").asText()).isEqualTo("mock");

        mvc.perform(put("/api/v1/decisions/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(input(40, 55000, 3, "增加视觉识别功能"))).andExpect(status().isOk());
        JsonNode second = json(mvc.perform(post("/api/v1/decisions/" + id + "/simulate"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(second.path("results").get(1).path("riskLevel").asText()).isEqualTo("high");
        JsonNode comparison = json(mvc.perform(post("/api/v1/decisions/" + id + "/compare")
                .contentType(MediaType.APPLICATION_JSON).content("{\"leftVersionId\":\"" + first.path("id").asText()
                        + "\",\"rightVersionId\":\"" + second.path("id").asText() + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(comparison.path("changedInputs").size()).isEqualTo(2);
        assertThat(comparison.path("riskChange").asText()).isEqualTo("medium → high");

        JsonNode history = json(mvc.perform(get("/api/v1/decisions/" + id + "/versions"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(history.size()).isEqualTo(2);
        JsonNode restored = json(mvc.perform(post("/api/v1/decisions/" + id + "/restore")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"versionId\":\"" + first.path("id").asText() + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(restored.path("timeLimitDays").asInt()).isEqualTo(60);
        assertThat(restored.path("latestResults").isEmpty()).isTrue();
        mvc.perform(get("/api/v1/decisions/" + id)).andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidDataAndMissingScenario() throws Exception {
        mvc.perform(post("/api/v1/decisions").contentType(MediaType.APPLICATION_JSON)
                .content(input(0, 100000, 0, ""))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/decisions/not-present")).andExpect(status().isNotFound());
    }

    @Test
    void editsRelationAndKeepsGraphIdsScopedAcrossScenarios() throws Exception {
        JsonNode first = json(mvc.perform(post("/api/v1/decisions").contentType(MediaType.APPLICATION_JSON)
                .content(input(60, 100000, 3, ""))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        JsonNode second = json(mvc.perform(post("/api/v1/decisions").contentType(MediaType.APPLICATION_JSON)
                .content(input(60, 100000, 3, ""))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        assertThat(first.path("id").asText()).isNotEqualTo(second.path("id").asText());
        String modified = """
                {"title":"关系修订","goal":"交付演示项目","timeLimitDays":60,"budgetYuan":100000,
                 "peopleCount":3,"hasServer":true,"changeRequest":"",
                 "relations":[{"id":"people-goal","from":"people","to":"goal","label":"核心成员影响实施速度",
                                "confidence":0.7,"assumption":"按实际投入校准"}]}
                """;
        JsonNode updated = json(mvc.perform(put("/api/v1/decisions/" + second.path("id").asText())
                .contentType(MediaType.APPLICATION_JSON).content(modified))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode relation = updated.path("relations").get(0);
        assertThat(relation.path("id").asText()).isEqualTo("people-goal");
        assertThat(relation.path("label").asText()).isEqualTo("核心成员影响实施速度");
        assertThat(relation.path("source").asText()).isEqualTo("user");
        assertThat(relation.path("confidence").asDouble()).isEqualTo(0.7);
    }
}
