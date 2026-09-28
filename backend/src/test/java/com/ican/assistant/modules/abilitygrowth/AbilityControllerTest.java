package com.ican.assistant.modules.abilitygrowth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static com.ican.assistant.modules.abilitygrowth.AbilityDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AbilityControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AbilityMapper mapper;
    @Autowired AbilityService service;

    @Test
    void seedGraphMatchesFrontendContract() throws Exception {
        mvc.perform(get("/api/v1/ability/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("api"))
                .andExpect(jsonPath("$.nodes.length()").value(10))
                .andExpect(jsonPath("$.relations.length()").value(10))
                .andExpect(jsonPath("$.nodes[0].id").value("python"))
                .andExpect(jsonPath("$.nodes[0].evidenceIds[0]").value("ev-python"))
                .andExpect(jsonPath("$.evidence.length()").value(2))
                .andExpect(jsonPath("$.goal.title").isString());
    }

    @Test
    void newSkillAndUpdatesArePersistedWithMyBatis() throws Exception {
        mvc.perform(post("/api/v1/ability/skills").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(node("java", "Java", 1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nodes.length()").value(11));
        mvc.perform(put("/api/v1/ability/skills/java").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(node("java", "Java", 3))))
                .andExpect(status().isOk());
        assertThat(mapper.findSkills("demo-user")).filteredOn(row -> row.id().equals("java"))
                .singleElement().satisfies(row -> assertThat(row.level()).isEqualTo(3));
        mvc.perform(get("/api/v1/ability/graph"))
                .andExpect(jsonPath("$.nodes[10].id").value("java"))
                .andExpect(jsonPath("$.nodes[10].level").value(3));
    }

    @Test
    void postMergesCaseInsensitiveNameAndPreservesExistingIdAndPosition() throws Exception {
        mvc.perform(post("/api/v1/ability/skills").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(node("candidate-python", "  PYTHON  ", 4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(10))
                .andExpect(jsonPath("$.nodes[0].id").value("python"))
                .andExpect(jsonPath("$.nodes[0].level").value(4))
                .andExpect(jsonPath("$.nodes[0].x").value(48));
        assertThat(mapper.findSkills("demo-user")).noneMatch(row -> row.id().equals("candidate-python"));
    }

    @Test
    void updateRejectsMissingIdMismatchAndNameConflict() throws Exception {
        mvc.perform(put("/api/v1/ability/skills/missing").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(node("missing", "New skill", 1))))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/ability/skills/python").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(node("linux", "Linux", 1))))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/ability/skills/python").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(node("python", "Linux", 1))))
                .andExpect(status().isConflict());
    }

    @Test
    void relationWritesPersistAndDuplicateUpdatesConfidence() throws Exception {
        var relation = new SkillRelation("git", "docker", "prerequisite", 0.8);
        mvc.perform(post("/api/v1/ability/relations").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(relation)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.relations.length()").value(11));
        mvc.perform(post("/api/v1/ability/relations").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SkillRelation("git", "docker", "prerequisite", 0.9))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.relations.length()").value(11));
        assertThat(mapper.findRelations("demo-user"))
                .filteredOn(row -> row.fromId().equals("git") && row.toId().equals("docker"))
                .singleElement().satisfies(row -> assertThat(row.confidence()).isEqualTo(0.9));
    }

    @Test
    void danglingSelfAndCyclicRelationsAreRejectedBeforeWrite() throws Exception {
        for (var relation : List.of(new SkillRelation("missing", "python", "prerequisite", 1.0),
                new SkillRelation("python", "python", "related", 1.0),
                new SkillRelation("deploy", "python", "prerequisite", 1.0))) {
            mvc.perform(post("/api/v1/ability/relations").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(relation)))
                    .andExpect(status().isBadRequest());
        }
        assertThat(mapper.findRelations("demo-user")).hasSize(10);
    }

    @Test
    void evidenceAndSkillLinkArePersistedTogether() throws Exception {
        mvc.perform(post("/api/v1/ability/skills/python/evidence").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"practice-new","title":"接口实践","note":"完成参数校验"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.evidence.length()").value(3));
        assertThat(mapper.findEvidenceLinks("demo-user"))
                .anyMatch(link -> link.skillId().equals("python") && link.evidenceId().equals("practice-new"));
        assertThat(mapper.findEvidence("demo-user")).anyMatch(row -> row.id().equals("practice-new"));
    }

    @Test
    void validatesNestedPathNodesAndSkillFields() throws Exception {
        mvc.perform(post("/api/v1/ability/skills").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(node("invalid", "", 5))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").isString());
        mvc.perform(post("/api/v1/ability/path").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PathRequest(List.of(node("invalid", "Skill", 8)), List.of()))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/ability/path").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nodes\":[null],\"relations\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/ability/skills").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(node("x".repeat(65), "Skill", 1))))
                .andExpect(status().isBadRequest());
        var badEvidence = new SkillNode("python", "Python", "", 2, "mastered", List.of("unknown"), 1.0, 1.0);
        mvc.perform(post("/api/v1/ability/skills").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(badEvidence)))
                .andExpect(status().isBadRequest());
        assertThat(mapper.findSkills("demo-user")).hasSize(10);
    }

    @Test
    void parseIsDeterministicAndDoesNotPersistCandidates() throws Exception {
        String input = "{\"input\":\"我会 Java，不会 Docker，正在学习 MySQL\"}";
        String first = mvc.perform(post("/api/v1/ability/parse").contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isOk()).andExpect(jsonPath("$.suggestedNodes.length()").value(3))
                .andExpect(jsonPath("$.suggestedNodes[1].level").value(0))
                .andReturn().getResponse().getContentAsString();
        String second = mvc.perform(post("/api/v1/ability/parse").contentType(MediaType.APPLICATION_JSON).content(input))
                .andReturn().getResponse().getContentAsString();
        assertThat(second).isEqualTo(first);
        assertThat(mapper.findSkills("demo-user")).hasSize(10);
    }

    @Test
    void pathEndpointUsesOnlyPrerequisitesFromSubmittedSnapshot() throws Exception {
        AbilityGraph graph = service.graph();
        String response = mvc.perform(post("/api/v1/ability/path").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PathRequest(graph.nodes(), graph.relations()))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        GrowthPathStep[] path = json.readValue(response, GrowthPathStep[].class);
        assertThat(path).filteredOn(step -> step.skillId().equals("shell")).singleElement().satisfies(step -> {
            assertThat(step.prerequisiteIds()).containsExactly("linux");
            assertThat(step.status()).isEqualTo("next");
        });
        assertThat(path).filteredOn(step -> step.skillId().equals("docker"))
                .singleElement().satisfies(step -> assertThat(step.status()).isEqualTo("blocked"));
    }

    private static SkillNode node(String id, String name, int level) {
        return new SkillNode(id, name, "测试能力", level, "developing", List.of(), 120.0, 200.0);
    }
}
