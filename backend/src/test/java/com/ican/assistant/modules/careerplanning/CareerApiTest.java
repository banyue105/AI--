package com.ican.assistant.modules.careerplanning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CareerApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void filtersDemoJobsAndMatchesCurrentAbility() throws Exception {
        mvc.perform(get("/api/v1/career/jobs").param("category", "后端开发").param("city", "上海"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("job-backend-1"));
        mvc.perform(get("/api/v1/career/matches")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].match.score").isNumber());
        mvc.perform(put("/api/v1/career/preferences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"后端开发\",\"city\":\"上海\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.city").value("上海"));
        mvc.perform(get("/api/v1/career/preferences")).andExpect(jsonPath("$.category").value("后端开发"));
    }

    @Test
    void targetAndReportAreSavedAsUserScopedSnapshots() throws Exception {
        mvc.perform(put("/api/v1/career/target").contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobId\":\"job-backend-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.targetJobId").value("job-backend-1"));
        mvc.perform(get("/api/v1/career/target")).andExpect(jsonPath("$.job.title").value("后端开发工程师"));

        String body = """
                {"jobId":"job-backend-1","jdText":"后端开发：熟悉 Python、HTTP、SQL、Git，了解 Docker 和 Linux。"}
                """;
        String response = mvc.perform(post("/api/v1/career/reports").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.jobTitle").value("后端开发工程师"))
                .andExpect(jsonPath("$.matched.length()").isNumber())
                .andExpect(jsonPath("$.gaps.length()").isNumber())
                .andExpect(jsonPath("$.actions.length()").isNumber())
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(response).get("id").asText();
        assertThat(json.readTree(response).get("recognizedSkills").toString()).contains("Python", "Docker");
        assertThat(json.readTree(response).get("actions").toString()).contains("Dockerfile", "HTTP 接口");

        mvc.perform(get("/api/v1/career/reports/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/v1/career/reports")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/career/reports/" + id).header("X-User-Id", "other-user"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/career/preferences").header("X-User-Id", "other-user"))
                .andExpect(jsonPath("$.targetJobId").doesNotExist());
    }

    @Test
    void invalidJobAndShortJdAreRejected() throws Exception {
        mvc.perform(put("/api/v1/career/target").contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobId\":\"not-a-job\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/career/reports").contentType(MediaType.APPLICATION_JSON)
                .content("{\"jdText\":\"太短\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void newlyAddedAbilityCanBeRecognizedInCustomJd() throws Exception {
        mvc.perform(post("/api/v1/ability/skills").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":"k8s","name":"Kubernetes","description":"容器编排",
                         "level":3,"status":"mastered","evidenceIds":[],"x":100,"y":100}
                        """))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/career/reports").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"jobTitle":"平台工程师","jdText":"负责 Kubernetes 平台的部署与维护，具备 Kubernetes 集群实践经验。"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recognizedSkills[0]").value("Kubernetes"))
                .andExpect(jsonPath("$.matched[0]").value("Kubernetes（能力等级 3/4）"));
    }
}
