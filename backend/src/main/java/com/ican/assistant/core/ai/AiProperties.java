package com.ican.assistant.core.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the optional OpenAI-compatible text provider. */
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    /** mock (default) or http/openai. */
    private String provider = "mock";
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String model = "gpt-4o-mini";
    private int timeoutMs = 30_000;
    private boolean fallbackToMock = true;
    private boolean disableThinking = false;

    public AiProperties() {}

    public AiProperties(String provider, String baseUrl, String apiKey, String model, int timeoutSeconds) {
        this(provider, baseUrl, apiKey, model, timeoutSeconds, true);
    }

    public AiProperties(String provider, String baseUrl, String apiKey, String model,
                         int timeoutSeconds, boolean fallbackToMock) {
        this.provider = provider == null || provider.isBlank() ? "mock" : provider;
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model == null || model.isBlank() ? "gpt-4o-mini" : model;
        this.timeoutMs = Math.max(1, timeoutSeconds) * 1000;
        this.fallbackToMock = fallbackToMock;
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public boolean isFallbackToMock() { return fallbackToMock; }
    public void setFallbackToMock(boolean fallbackToMock) { this.fallbackToMock = fallbackToMock; }
    public boolean isDisableThinking() { return disableThinking; }
    public void setDisableThinking(boolean disableThinking) { this.disableThinking = disableThinking; }
    public boolean enabled() { return !"mock".equalsIgnoreCase(provider) && apiKey != null && !apiKey.isBlank(); }
    public String baseUrl() { return baseUrl; }
    public String apiKey() { return apiKey; }
    public String model() { return model; }
    public int timeoutSeconds() { return Math.max(1, timeoutMs / 1000); }
}
