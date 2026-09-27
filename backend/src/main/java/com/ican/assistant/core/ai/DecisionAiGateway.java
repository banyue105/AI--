package com.ican.assistant.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.ParseResult;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Suggestion;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.SimulationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** AI is limited to extraction and narrative; numeric simulation is exclusively DecisionRules. */
@Component
public class DecisionAiGateway {
    private final MockDecisionAiProvider mock;
    private final ObjectMapper mapper;
    private final RestClient client;
    private final String apiKey;
    private final String model;

    public DecisionAiGateway(MockDecisionAiProvider mock, ObjectMapper mapper,
                             @Value("${app.ai.api-key:}") String apiKey,
                             @Value("${app.ai.base-url:https://api.openai.com/v1}") String baseUrl,
                             @Value("${app.ai.model:gpt-4o-mini}") String model) {
        this.mock = mock;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = model;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ParseResult parse(String input) {
        if (apiKey.isBlank()) return mock.parse(input);
        try {
            String body = complete("只提取用户明确写出的项目条件。不要推断数字。kind 仅可为 people,budget,time,server,change。预算统一为元，期限统一为天。",
                    input, "decision_parse", parseSchema());
            JsonNode data = mapper.readTree(body);
            List<Suggestion> suggestions = new ArrayList<>();
            for (JsonNode item : data.path("candidates")) {
                String kind = item.path("kind").asText();
                if (!List.of("people", "budget", "time", "server", "change").contains(kind)) continue;
                suggestions.add(new Suggestion(kind, item.path("label").asText(), item.path("value").asText(),
                        Math.max(0, Math.min(1, item.path("confidence").asDouble())),
                        item.path("assumption").asText(), "ai"));
            }
            List<String> assumptions = new ArrayList<>();
            data.path("assumptions").forEach(value -> assumptions.add(value.asText()));
            return new ParseResult(data.path("summary").asText(), suggestions, assumptions, "ai");
        } catch (Exception ignored) {
            ParseResult fallback = mock.parse(input);
            return new ParseResult(fallback.summary(), fallback.candidates(),
                    List.of("AI 服务暂时不可用，已切换为确定性提取；请仔细核对候选条件。"), "mock");
        }
    }

    public Explanation explain(SimulationResult baseline, SimulationResult changed) {
        if (!apiKey.isBlank()) {
            try {
                String body = complete("仅解释给定规则结果，不得新增、修改或计算数字。简体中文，80字以内。",
                        mapper.writeValueAsString(Map.of("baseline", baseline, "changed", changed)),
                        "decision_explanation", explanationSchema());
                String explanation = mapper.readTree(body).path("explanation").asText();
                if (!explanation.isBlank()) return new Explanation(explanation, "ai");
            } catch (Exception ignored) {
                // The deterministic explanation keeps simulation available during AI outages.
            }
        }
        return new Explanation(mock.explain(baseline, changed), "mock");
    }

    private String complete(String instructions, String input, String name, Map<String, Object> schema) {
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "system", "content", instructions), Map.of("role", "user", "content", input)),
                "response_format", Map.of("type", "json_schema", "json_schema", Map.of("name", name, "strict", true, "schema", schema)));
        JsonNode response = client.post().uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey).contentType(MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(JsonNode.class);
        if (response == null) throw new IllegalStateException("AI 未返回结果");
        return response.path("choices").path(0).path("message").path("content").asText();
    }

    private Map<String, Object> parseSchema() {
        Map<String, Object> string = Map.of("type", "string");
        Map<String, Object> item = Map.of("type", "object", "additionalProperties", false,
                "properties", Map.of("kind", string, "label", string, "value", string,
                        "confidence", Map.of("type", "number"), "assumption", string),
                "required", List.of("kind", "label", "value", "confidence", "assumption"));
        return Map.of("type", "object", "additionalProperties", false,
                "properties", Map.of("summary", string, "candidates", Map.of("type", "array", "items", item),
                        "assumptions", Map.of("type", "array", "items", string)),
                "required", List.of("summary", "candidates", "assumptions"));
    }

    private Map<String, Object> explanationSchema() {
        return Map.of("type", "object", "additionalProperties", false,
                "properties", Map.of("explanation", Map.of("type", "string")), "required", List.of("explanation"));
    }

    public record Explanation(String text, String source) {}
}
