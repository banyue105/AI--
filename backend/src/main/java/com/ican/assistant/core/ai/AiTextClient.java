package com.ican.assistant.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Small OpenAI-compatible client. Invalid or unavailable model responses are treated as a fallback signal. */
@Component
@EnableConfigurationProperties(AiProperties.class)
public class AiTextClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    public AiTextClient(ObjectMapper objectMapper, AiProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeoutSeconds() * 1000);
        requestFactory.setReadTimeout(properties.timeoutSeconds() * 1000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public Optional<JsonNode> completeJson(String systemPrompt, String userPrompt) {
        if (!enabled()) return Optional.empty();
        try {
            String base = properties.baseUrl().replaceAll("/+$", "");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", properties.model());
            payload.put("temperature", 0.15);
            payload.put("response_format", Map.of("type", "json_object"));
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)));
            if (properties.isDisableThinking()) payload.put("enable_thinking", false);
            JsonNode response = restClient.post()
                    .uri(base + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) return Optional.empty();
            String content = response.at("/choices/0/message/content").asText("").trim();
            if (content.isBlank()) return Optional.empty();
            return Optional.of(objectMapper.readTree(stripMarkdownFence(content)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public Duration timeout() {
        return Duration.ofSeconds(properties.timeoutSeconds());
    }

    private String stripMarkdownFence(String content) {
        if (!content.startsWith("```")) return content;
        int firstLine = content.indexOf('\n');
        int lastFence = content.lastIndexOf("```");
        if (firstLine >= 0 && lastFence > firstLine) return content.substring(firstLine + 1, lastFence).trim();
        return content;
    }
}
