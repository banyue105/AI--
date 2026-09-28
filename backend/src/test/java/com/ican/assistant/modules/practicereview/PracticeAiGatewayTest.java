package com.ican.assistant.modules.practicereview;

import static com.ican.assistant.modules.practicereview.PracticeDtos.*;
import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ican.assistant.core.ai.MockPracticeAiProvider;
import com.ican.assistant.core.ai.PracticeAiGateway;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PracticeAiGatewayTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final MockPracticeAiProvider mock = new MockPracticeAiProvider();
    private HttpServer server;
    private java.util.concurrent.ExecutorService executor;
    private Project project() {
        return new Project("p", "部署实践", "部署网络服务", PracticeTemplates.NETWORK, 2, PracticeTemplates.network().criteria(),
                List.of(new Evidence("e", 1, "原始日志", "log", "HTTP/1.1 200 OK\nrequest_id=test-001", List.of(), Instant.now())),
                List.of(), "", null, Instant.now(), Instant.now(), List.of());
    }
    private PracticeAiGateway gateway(Object responseContent, int delay, int timeout) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool(); server.setExecutor(executor);
        byte[] body = mapper.writeValueAsBytes(Map.of("choices", List.of(Map.of("message", Map.of("content", mapper.writeValueAsString(responseContent))))));
        server.createContext("/v1/chat/completions", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                if (delay > 0) Thread.sleep(delay);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body);
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        return new PracticeAiGateway(mock, new PracticeRules(), mapper, "local-test-key",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test-model", timeout);
    }
    @AfterEach void close() { if (server != null) server.stop(0); if (executor != null) executor.shutdownNow(); }

    @Test void compliantRemoteResultIsValidatedAndMarkedAi() throws Exception {
        Project project = project();
        var gateway = gateway(Map.of("findings", mock.review(project)), 0, 2000);
        var result = gateway.review(project);
        assertThat(result.source()).isEqualTo("ai"); assertThat(result.providerNotice()).isNull();
        assertThat(result.findings()).isEqualTo(mock.review(project));
    }
    @Test void fabricatedCitationFallsBackWithoutShowingTheFakeQuote() throws Exception {
        Project project = project();
        var json = mapper.valueToTree(Map.of("findings", mock.review(project)));
        ((ObjectNode) json.path("findings").get(0).path("citations").get(0)).put("quote", "invented evidence");
        var result = gateway(json, 0, 2000).review(project);
        assertThat(result.source()).isEqualTo("mock"); assertThat(result.providerNotice()).contains("引用不合规");
        assertThat(result.findings()).isEqualTo(mock.review(project));
    }
    @Test void timeoutFallsBackToTheCompleteOriginalMaterials() throws Exception {
        Project project = project();
        var result = gateway(Map.of("findings", mock.review(project)), 800, 100).review(project);
        assertThat(result.source()).isEqualTo("mock"); assertThat(result.providerNotice()).contains("超时");
        assertThat(result.findings().getFirst().citations().getFirst().quote()).isEqualTo("HTTP/1.1 200 OK");
    }
    @Test void invalidSuggestionSchemaReturnsExplicitTemplateCandidates() throws Exception {
        var result = gateway(Map.of("candidates", List.of(Map.of("title", "虚构", "standard", "通过", "expectedEvidence", "日志", "required", "true"))), 0, 2000).suggest(project());
        assertThat(result.source()).isEqualTo("mock"); assertThat(result.providerNotice()).contains("输出不合规");
        assertThat(result.candidates()).hasSize(4);
    }
}
