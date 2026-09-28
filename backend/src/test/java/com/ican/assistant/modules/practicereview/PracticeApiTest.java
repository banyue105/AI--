package com.ican.assistant.modules.practicereview;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PracticeApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PracticeService service;
    @Autowired com.ican.assistant.modules.decisionsandbox.DecisionScenarioService decisions;

    private JsonNode response(org.springframework.test.web.servlet.ResultActions action) throws Exception {
        return mapper.readTree(action.andReturn().getResponse().getContentAsString());
    }
    private String body(Object input) throws Exception { return mapper.writeValueAsString(input); }
    private JsonNode create() throws Exception {
        return response(mvc.perform(post("/api/v1/practice/projects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"实践验收\",\"goal\":\"部署网络服务\",\"templateId\":\"network-service-v1\"}")).andExpect(status().isCreated()));
    }
    private JsonNode evidence(JsonNode project, String title, String kind, String content) throws Exception {
        return response(mvc.perform(post("/api/v1/practice/projects/" + project.path("id").asText() + "/evidence")
                .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("inputRevision", project.path("inputRevision").asInt(),
                        "title", title, "kind", kind, "content", content, "criterionIds", List.of())))).andExpect(status().isCreated()));
    }
    private JsonNode review(JsonNode project, String requestId) throws Exception {
        return response(mvc.perform(post("/api/v1/practice/projects/" + project.path("id").asText() + "/reviews")
                .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("inputRevision", project.path("inputRevision").asInt(), "requestId", requestId))))
                .andExpect(status().isCreated()));
    }

    @Test void completeFlowPersistsHistoryConfirmsAndSavesIdempotentReferenceFeedback() throws Exception {
        JsonNode project = create(); String id = project.path("id").asText(); String base = "/api/v1/practice/projects/" + id;
        project = evidence(project, "访问", "log", "HTTP/1.1 200 OK\r\nrequest_id=demo-001");
        String requestId = UUID.randomUUID().toString(); JsonNode first = review(project, requestId);
        JsonNode repeat = review(project, requestId);
        assertThat(repeat.path("id").asText()).isEqualTo(first.path("id").asText());
        assertThat(first.path("findings").get(1).path("verdict").asText()).isEqualTo("insufficient");
        assertThat(first.path("input").path("evidence").get(0).path("content").asText()).doesNotContain("\r");
        project = evidence(project, "TLS", "config", "listen 443 ssl;\nssl_certificate /demo/cert;\nssl_certificate_key /demo/key;");
        project = evidence(project, "HTTPS", "log", "HTTPS GET https://demo.local/health -> 200");
        project = evidence(project, "完整说明", "note", "环境：Ubuntu 24.04\n启动：npm run start\n验证：curl https://demo.local/health");
        JsonNode old = response(mvc.perform(get(base + "/reviews/" + first.path("id").asText())).andExpect(status().isOk()));
        assertThat(old.path("isStale").asBoolean()).isTrue(); assertThat(old.path("input").path("evidence").size()).isEqualTo(1);
        mvc.perform(post(base + "/reviews/" + first.path("id").asText() + "/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("inputRevision", project.path("inputRevision").asInt(), "overrides", List.of())))).andExpect(status().isConflict());
        JsonNode current = review(project, UUID.randomUUID().toString()); String reviewId = current.path("id").asText();
        for (JsonNode item : current.path("findings")) assertThat(item.path("verdict").asText()).isEqualTo("supported");
        String confirm = body(Map.of("inputRevision", project.path("inputRevision").asInt(), "overrides", List.of()));
        JsonNode confirmed = response(mvc.perform(post(base + "/reviews/" + reviewId + "/confirm").contentType(MediaType.APPLICATION_JSON).content(confirm)).andExpect(status().isOk()));
        assertThat(confirmed.path("confirmation").path("confirmedAt").asText()).isNotBlank();
        mvc.perform(post(base + "/reviews/" + reviewId + "/confirm").contentType(MediaType.APPLICATION_JSON).content(confirm)).andExpect(status().isOk());
        mvc.perform(post(base + "/feedback").contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                "reviewId", reviewId, "requestId", UUID.randomUUID().toString(), "items", List.of(
                        Map.of("target", "ability", "title", "缺少技能名称", "note", "不能关联技能", "criterionIds", List.of(), "evidenceIds", List.of()))))))
                .andExpect(status().isBadRequest());
        String feedbackRequest = body(Map.of("reviewId", reviewId, "requestId", UUID.randomUUID().toString(), "items", List.of(
                Map.of("target", "ability", "targetName", "网络服务部署", "title", "部署实践证据", "note", "用户确认后保存为参考", "criterionIds", List.of(), "evidenceIds", List.of()),
                Map.of("target", "decision", "title", "实际投入参考", "note", "尚未自动更新估算参数", "criterionIds", List.of(), "evidenceIds", List.of()))));
        JsonNode saved = response(mvc.perform(post(base + "/feedback").contentType(MediaType.APPLICATION_JSON).content(feedbackRequest)).andExpect(status().isCreated()));
        JsonNode retried = response(mvc.perform(post(base + "/feedback").contentType(MediaType.APPLICATION_JSON).content(feedbackRequest)).andExpect(status().isCreated()));
        assertThat(saved).isEqualTo(retried); assertThat(saved.size()).isEqualTo(2);
        assertThat(saved.get(0).path("delivery").asText()).isEqualTo("reference_only");
        assertThat(saved.get(0).path("targetName").asText()).isEqualTo("网络服务部署");
        assertThat(response(mvc.perform(get(base + "/reviews")).andExpect(status().isOk())).size()).isEqualTo(2);
        assertThat(response(mvc.perform(get(base + "/feedback")).andExpect(status().isOk())).size()).isEqualTo(2);
    }

    @Test void evidenceRemovalKeepsOldQuotesAndForeignObjectsCannotBeUsed() throws Exception {
        JsonNode project = evidence(create(), "日志", "log", "HTTP/1.1 200 OK"); String base = "/api/v1/practice/projects/" + project.path("id").asText();
        JsonNode review = review(project, UUID.randomUUID().toString()); String evidenceId = project.path("evidence").get(0).path("id").asText();
        mvc.perform(delete(base + "/evidence/" + evidenceId).header("Origin", "http://127.0.0.1:5173")
                .param("inputRevision", project.path("inputRevision").asText())).andExpect(status().isOk());
        JsonNode historic = response(mvc.perform(get(base + "/reviews/" + review.path("id").asText())).andExpect(status().isOk()));
        assertThat(historic.path("findings").get(0).path("citations").get(0).path("quote").asText()).isEqualTo("HTTP/1.1 200 OK");
        JsonNode other = create();
        mvc.perform(get("/api/v1/practice/projects/" + other.path("id").asText() + "/reviews/" + review.path("id").asText())).andExpect(status().isNotFound());
        mvc.perform(post(base + "/evidence").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("inputRevision", project.path("inputRevision").asInt(),
                "title", "失效版本", "kind", "log", "content", "日志", "criterionIds", List.of())))).andExpect(status().isConflict());
    }

    @Test void rejectsForgedManualCitationsAndOversizedBodies() throws Exception {
        JsonNode project = create(); JsonNode reviewed = review(project, UUID.randomUUID().toString());
        String base = "/api/v1/practice/projects/" + project.path("id").asText();
        var forged = Map.of("criterionId", project.path("criteria").get(0).path("id").asText(), "verdict", "supported", "reason", "人工判断",
                "citations", List.of(Map.of("evidenceId", "unknown", "evidenceRevision", 1, "startLine", 1, "endLine", 1, "quote", "fake")));
        mvc.perform(post(base + "/reviews/" + reviewed.path("id").asText() + "/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("inputRevision", project.path("inputRevision").asInt(), "overrides", List.of(forged))))).andExpect(status().isBadRequest());
        mvc.perform(post(base + "/evidence").contentType(MediaType.APPLICATION_JSON).content("x".repeat(1024 * 1024 + 1))).andExpect(status().isPayloadTooLarge());
    }

    @Test void manualCorrectionRetainsModelResultAndCriteriaRemovalKeepsHistoricalInput() throws Exception {
        JsonNode project = evidence(create(), "运行日志", "log", "HTTP/1.1 200 OK\nHTTPS handshake failed");
        String base = "/api/v1/practice/projects/" + project.path("id").asText();
        JsonNode reviewed = review(project, UUID.randomUUID().toString());
        String evidenceId = project.path("evidence").get(0).path("id").asText();
        var corrected = Map.of("criterionId", project.path("criteria").get(1).path("id").asText(), "verdict", "insufficient",
                "reason", "该失败日志属于旧环境，需要补验当前环境", "citations", List.of(Map.of("evidenceId", evidenceId,
                        "evidenceRevision", 1, "startLine", 2, "endLine", 2, "quote", "HTTPS handshake failed")));
        String confirmation = body(Map.of("inputRevision", project.path("inputRevision").asInt(), "overrides", List.of(corrected)));
        JsonNode saved = response(mvc.perform(post(base + "/reviews/" + reviewed.path("id").asText() + "/confirm")
                .contentType(MediaType.APPLICATION_JSON).content(confirmation)).andExpect(status().isOk()));
        assertThat(saved.path("findings").get(1).path("verdict").asText()).isEqualTo("conflicting");
        assertThat(saved.path("confirmation").path("overrides").get(0).path("verdict").asText()).isEqualTo("insufficient");
        mvc.perform(post(base + "/reviews/" + reviewed.path("id").asText() + "/confirm").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("inputRevision", project.path("inputRevision").asInt(), "overrides", List.of())))).andExpect(status().isConflict());
        var criterion = project.path("criteria").get(0);
        JsonNode changed = response(mvc.perform(put(base + "/criteria").contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                "inputRevision", project.path("inputRevision").asInt(), "criteria", List.of(Map.of("id", criterion.path("id").asText(),
                        "title", criterion.path("title").asText(), "standard", criterion.path("standard").asText(),
                        "expectedEvidence", criterion.path("expectedEvidence").asText(), "required", true)))))).andExpect(status().isOk()));
        assertThat(changed.path("criteria").size()).isEqualTo(1);
        var historical = response(mvc.perform(get(base + "/reviews/" + reviewed.path("id").asText())).andExpect(status().isOk()));
        assertThat(historical.path("input").path("criteria").size()).isEqualTo(4);
        assertThat(historical.path("confirmation").path("overrides").get(0).path("reason").asText()).contains("旧环境");
    }

    @Test void selectedDecisionOptionIsFrozenAndLaterSandboxChangesDoNotAlterIt() {
        var original = new com.ican.assistant.modules.decisionsandbox.DecisionDtos.ScenarioInput("来源方案", "交付网页项目",
                60, new java.math.BigDecimal("100000"), 3, true, "增加视觉识别功能", List.of(), List.of());
        var scenario = decisions.create(original); var version = decisions.simulate(scenario.id());
        var project = service.create(new PracticeDtos.CreateRequest("来源快照实践", "验证部署结果", PracticeTemplates.NETWORK));
        var linked = service.linkDecision(project.id(), new PracticeDtos.OriginRequest(project.inputRevision(), scenario.id(), version.id(), "changed"));
        var source = version.results().stream().filter(option -> option.optionKey().equals("changed")).findFirst().orElseThrow();
        assertThat(linked.decisionOrigin().optionKey()).isEqualTo("changed");
        assertThat(linked.decisionOrigin().snapshot().optionName()).isEqualTo(source.optionName());
        assertThat(linked.decisionOrigin().snapshot().timeRange().min()).isEqualTo(source.timeRange().min());
        assertThat(linked.metrics()).isEmpty();
        decisions.update(scenario.id(), new com.ican.assistant.modules.decisionsandbox.DecisionDtos.ScenarioInput("来源已修改", "另一个目标",
                30, new java.math.BigDecimal("50000"), 2, false, "", List.of(), List.of()));
        decisions.simulate(scenario.id());
        assertThat(service.get(project.id()).decisionOrigin()).isEqualTo(linked.decisionOrigin());
        var relinked = service.linkDecision(project.id(), new PracticeDtos.OriginRequest(linked.inputRevision(), scenario.id(), version.id(), "changed"));
        assertThat(relinked.inputRevision()).isEqualTo(linked.inputRevision());
        assertThat(relinked.decisionOrigin()).isEqualTo(linked.decisionOrigin());
        assertThatThrownBy(() -> service.linkDecision(project.id(), new PracticeDtos.OriginRequest(linked.inputRevision(), scenario.id(), "unknown", "baseline")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test void unicodeCodePointBoundariesRemainValidInDatabaseStorage() throws Exception {
        String title = "😀".repeat(120), goal = "😀".repeat(1000), process = "😀".repeat(5000);
        JsonNode project = response(mvc.perform(post("/api/v1/practice/projects").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("title", title, "goal", goal, "templateId", PracticeTemplates.NETWORK)))).andExpect(status().isCreated()));
        String base = "/api/v1/practice/projects/" + project.path("id").asText();
        project = response(mvc.perform(put(base).contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                "inputRevision", project.path("inputRevision").asInt(), "title", title, "goal", goal, "metrics", List.of(), "processNote", process))))
                .andExpect(status().isOk()));
        var criterion = project.path("criteria").get(0);
        project = response(mvc.perform(put(base + "/criteria").contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                "inputRevision", project.path("inputRevision").asInt(), "criteria", List.of(Map.of("id", criterion.path("id").asText(),
                        "title", title, "standard", goal, "expectedEvidence", goal, "required", true)))))).andExpect(status().isOk()));
        project = evidence(project, title, "note", "😀".repeat(12000));
        var stored = response(mvc.perform(get(base)).andExpect(status().isOk()));
        assertThat(stored.path("title").asText()).isEqualTo(title);
        assertThat(stored.path("goal").asText()).isEqualTo(goal);
        assertThat(stored.path("processNote").asText()).isEqualTo(process);
        assertThat(stored.path("criteria").get(0).path("standard").asText()).isEqualTo(goal);
        assertThat(stored.path("evidence").get(0).path("content").asText()).isEqualTo("😀".repeat(12000));
        mvc.perform(post("/api/v1/practice/projects").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("title", title + "😀", "goal", "超长标题", "templateId", PracticeTemplates.NETWORK))))
                .andExpect(status().isBadRequest());
    }
}
