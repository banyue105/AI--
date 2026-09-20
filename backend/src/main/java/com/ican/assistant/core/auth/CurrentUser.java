package com.ican.assistant.core.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Temporary user boundary used until Spring Security authentication is added.
 * The login layer can later replace the header lookup with an authenticated principal
 * without changing module services or mapper queries.
 */
@Component
public class CurrentUser {
    public static final String HEADER = "X-User-Id";
    public static final String DEMO_USER_ID = "demo-user";

    private final HttpServletRequest request;

    public CurrentUser(HttpServletRequest request) {
        this.request = request;
    }

    public String id() {
        String value = request.getHeader(HEADER);
        if (value == null || value.isBlank()) return DEMO_USER_ID;
        String normalized = value.strip();
        if (!normalized.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")) return DEMO_USER_ID;
        return normalized;
    }
}
