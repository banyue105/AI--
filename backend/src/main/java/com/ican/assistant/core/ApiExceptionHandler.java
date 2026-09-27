package com.ican.assistant.core;

import com.ican.assistant.modules.decisionsandbox.DecisionCalculationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException exception) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "请检查输入条件。", details);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> invalid(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception instanceof IllegalArgumentException ? exception.getMessage() : "请求格式不正确。", Map.of());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> missing(NoSuchElementException exception) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DecisionCalculationException.class)
    public ResponseEntity<?> calculation(DecisionCalculationException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "CALCULATION_FAILED", exception.getMessage(), Map.of());
    }

    private ResponseEntity<?> error(HttpStatus status, String code, String message, Map<String, ?> details) {
        return ResponseEntity.status(status).body(Map.of("error", Map.of("code", code, "message", message, "details", details)));
    }
}
