package com.ican.assistant.modules.practicereview;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounds actual UTF-8 request bytes, including requests without Content-Length. */
@Component
public class PracticeBodyLimitFilter extends OncePerRequestFilter {
    private static final int MAX_BYTES = 1024 * 1024;

    @java.lang.Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/practice/")
                || !(request.getMethod().equals("POST") || request.getMethod().equals("PUT"));
    }

    @java.lang.Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        byte[] body = request.getContentLengthLong() > MAX_BYTES ? null : request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (body == null || body.length > MAX_BYTES) {
            response.setStatus(413); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":{\"code\":\"PAYLOAD_TOO_LARGE\",\"message\":\"请求材料超过 1 MiB，请缩减后重试。\",\"details\":{}}}");
            return;
        }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @java.lang.Override public ServletInputStream getInputStream() {
                ByteArrayInputStream input = new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    @java.lang.Override public int read() { return input.read(); }
                    @java.lang.Override public int read(byte[] buffer, int offset, int length) { return input.read(buffer, offset, length); }
                    @java.lang.Override public boolean isFinished() { return input.available() == 0; }
                    @java.lang.Override public boolean isReady() { return true; }
                    @java.lang.Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("Only blocking request reads are supported."); }
                };
            }
            @java.lang.Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)); }
        }, response);
    }
}
