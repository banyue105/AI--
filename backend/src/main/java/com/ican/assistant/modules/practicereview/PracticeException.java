package com.ican.assistant.modules.practicereview;

import org.springframework.http.HttpStatus;

public class PracticeException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public PracticeException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }

    public static PracticeException conflict(String code, String message) {
        return new PracticeException(HttpStatus.CONFLICT, code, message);
    }
}
