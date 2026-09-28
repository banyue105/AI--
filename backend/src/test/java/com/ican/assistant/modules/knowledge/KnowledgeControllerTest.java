package com.ican.assistant.modules.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class KnowledgeControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private KnowledgeTrackMapper mapper;

    @Test
    void catalogContainsEveryServerOwnedPresetDirection() throws Exception {
        mvc.perform(get("/api/v1/knowledge/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[*].id", hasItems(
                        "frontend", "backend", "network", "data-analyst", "ml-engineer", "devops", "mobile", "data-engineer", "product-manager", "cybersecurity")))
                .andExpect(jsonPath("$[0].source").value("catalog"))
                .andExpect(jsonPath("$[0].stages", hasSize(3)));
    }

    @Test
    void dataAnalysisRequestSelectsCatalogTrackWithoutPersistingCustomDirection() throws Exception {
        String response = generate("数据分析工程师，学习 SQL、Python 和可视化");
        KnowledgeTrack track = objectMapper.readValue(response, KnowledgeTrack.class);
        assertThat(track.id()).isEqualTo("data-analyst");
        assertThat(track.source()).isEqualTo("catalog");
        assertThat(track.stages()).hasSize(3);
        assertThat(mapper.findAllJson()).isEmpty();
    }

    @Test
    void equivalentDirectionsResolveToSameCatalogTrack() throws Exception {
        String first = generate("BI Analyst");
        String second = generate("数据看板分析");
        assertThat(objectMapper.readTree(first).get("id").asText()).isEqualTo("data-analyst");
        assertThat(objectMapper.readTree(second).get("id").asText()).isEqualTo("data-analyst");
        assertThat(mapper.findAllJson()).isEmpty();
    }

    @Test
    void unmatchedDirectionRequiresAListedTrack() throws Exception {
        mvc.perform(post("/api/v1/knowledge/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("query", "用户体验设计", "currentSkills", List.of()))))
                .andExpect(status().isUnprocessableEntity());
        assertThat(mapper.findAllJson()).isEmpty();
    }

    @Test
    void invalidAndOversizedRequestsDoNotPersistTracks() throws Exception {
        List<String> invalidRequests = List.of(
                "{\"query\":\" \",\"currentSkills\":[]}",
                "{\"query\":\"Java\"}",
                "{\"query\":\"Java\",\"currentSkills\":[null]}",
                "{\"query\":\"Java\",\"currentSkills\":[{\"name\":\"SQL\",\"level\":5,\"status\":\"developing\"}]}",
                "{\"query\":\"Java\",\"currentSkills\":[{\"name\":\"\",\"level\":1,\"status\":\"unknown\"}]}",
                objectMapper.writeValueAsString(Map.of("query", "a".repeat(121), "currentSkills", List.of())));
        for (String request : invalidRequests) {
            mvc.perform(post("/api/v1/knowledge/generate")
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/knowledge/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"!!!\",\"currentSkills\":[]}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(mapper.findAllJson()).isEmpty();
    }

    private String generate(String query) throws Exception {
        return mvc.perform(post("/api/v1/knowledge/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("query", query, "currentSkills", List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("catalog"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
