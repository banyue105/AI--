package com.ican.assistant.modules.practicereview;

import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PracticeController.class)
public class PracticeApiAdvice {
    @ExceptionHandler(PracticeException.class)
    public ResponseEntity<?> practice(PracticeException exception) {
        return error(exception.status(), exception.code(), exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> persistence(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && ("23505".equals(sql.getSQLState()) || sql.getErrorCode() == 1062))
                return error(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "记录已由另一请求保存，请重新获取最新记录。");
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "PERSISTENCE_ERROR", "记录未能保存，请重新加载后重试。");
    }

    private ResponseEntity<?> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", Map.of("code", code, "message", message, "details", Map.of())));
    }
}
