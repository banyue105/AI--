package com.ican.assistant.modules.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.List;
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
    void catalogMatchesExistingFrontendTracks() throws Exception {
        mvc.perform(get("/api/v1/knowledge/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains("frontend", "backend", "network")))
                .andExpect(jsonPath("$[0].source").value("catalog"))
                .andExpect(jsonPath("$[0].stages", hasSize(3)))
                .andExpect(jsonPath("$[0].stages[0].items[0].name").value("HTML"))
                .andExpect(jsonPath("$[0].stages[0].items[0].aliases[0]").value("HTML5"));
    }

    @Test
    void generatedTrackIsPersistedAndIncludedInLaterCatalogRequests() throws Exception {
        String response = mvc.perform(post("/api/v1/knowledge/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"数据分析工程师", "currentSkills":[
                                    {"name":"SQL","level":2,"status":"developing"}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("mock"))
                .andExpect(jsonPath("$.description", containsString("本地候选")))
                .andExpect(jsonPath("$.description", containsString("真实 AI 模型尚未接入")))
                .andExpect(jsonPath("$.stages", hasSize(3)))
                .andExpect(jsonPath("$.stages[0].items[1].name").value("SQL"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        KnowledgeTrack generated = objectMapper.readValue(response, KnowledgeTrack.class);
        assertThat(objectMapper.readValue(mapper.findJsonById(generated.id()), KnowledgeTrack.class))
                .isEqualTo(generated);

        mvc.perform(get("/api/v1/knowledge/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].id", hasItem(generated.id())))
                .andExpect(jsonPath("$[3].source").value("mock"));
    }

    @Test
    void repeatedDirectionHasStableIdAndOnlyOneStoredEntry() throws Exception {
        String first = generate("  BI   Analyst  ");
        String second = generate("bi analyst!");
        assertThat(objectMapper.readTree(first).get("id").asText())
                .isEqualTo(objectMapper.readTree(second).get("id").asText());
        assertThat(mapper.findAllJson()).hasSize(1);
        mvc.perform(get("/api/v1/knowledge/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void arbitraryDirectionProducesValidThreeStageLocalTemplate() throws Exception {
        String response = generate("用户体验设计");
        KnowledgeTrack track = objectMapper.readValue(response, KnowledgeTrack.class);
        assertThat(track.title()).isEqualTo("用户体验设计");
        assertThat(track.stages()).hasSize(3).allSatisfy(stage -> {
            assertThat(stage.items()).hasSize(3).allSatisfy(item -> {
                assertThat(item.id()).isNotBlank();
                assertThat(item.priority()).isIn("required", "recommended");
                assertThat(item.aliases()).isNotNull();
            });
        });
    }

    @Test
    void invalidAndOversizedRequestsDoNotPersistTracks() throws Exception {
        List<String> invalidRequests = List.of(
                "{\"query\":\" \",\"currentSkills\":[]}",
                "{\"query\":\"!!!\",\"currentSkills\":[]}",
                "{\"query\":\"Java\"}",
                "{\"query\":\"Java\",\"currentSkills\":[null]}",
                "{\"query\":\"Java\",\"currentSkills\":[{\"name\":\"SQL\",\"level\":5,\"status\":\"developing\"}]}",
                "{\"query\":\"Java\",\"currentSkills\":[{\"name\":\"\",\"level\":1,\"status\":\"unknown\"}]}",
                objectMapper.writeValueAsString(Map.of("query", "a".repeat(121), "currentSkills", List.of())),
                objectMapper.writeValueAsString(Map.of("query", "Java", "currentSkills",
                        java.util.Collections.nCopies(201, Map.of("name", "SQL", "level", 1, "status", "gap")))));
        for (String request : invalidRequests) {
            mvc.perform(post("/api/v1/knowledge/generate")
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadRequest());
        }
        assertThat(mapper.findAllJson()).isEmpty();
    }

    private String generate(String query) throws Exception {
        return mvc.perform(post("/api/v1/knowledge/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("query", query, "currentSkills", List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("mock"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
