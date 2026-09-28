package com.ican.assistant.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkspaceControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void healthAndHomeReadTheDatabase() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.database").value("UP"));
        mvc.perform(get("/api/v1/home")).andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.id").value("demo-user"))
                .andExpect(jsonPath("$.modules.length()").value(4))
                .andExpect(jsonPath("$.modules[3].id").value("module4"));
    }

    @Test
    void updatingProfilePersistsAndUpdatesGraphGoal() throws Exception {
        mvc.perform(put("/api/v1/profile").contentType("application/json").content("""
                {"id":"demo-user","name":"测试用户","goals":["完成机器人项目","学习控制理论"]}
                """)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/profile")).andExpect(jsonPath("$.name").value("测试用户"))
                .andExpect(jsonPath("$.goals[1]").value("学习控制理论"));
        mvc.perform(get("/api/v1/ability/graph")).andExpect(jsonPath("$.goal.title").value("完成机器人项目"));
    }

    @Test
    void invalidProfileReturnsStructuredErrorWithoutMutating() throws Exception {
        mvc.perform(put("/api/v1/profile").contentType("application/json")
                .content("{\"name\":\"\",\"goals\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/profile")).andExpect(jsonPath("$.name").value("林澈"));
    }

    @Test
    void corsAcceptsOnlyConfiguredOrigins() throws Exception {
        mvc.perform(options("/api/v1/profile").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/v1/profile").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingRoutesAndUnsupportedMethodsKeepTheirHttpStatus() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mvc.perform(delete("/api/v1/profile")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }
}
