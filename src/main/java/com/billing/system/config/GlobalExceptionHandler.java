package com.billing.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Replaces Spring's default 500 page with a JSON body the frontend can show
 * to the user, and logs the full stack trace on the server. Validation errors
 * thrown from services as plain RuntimeException become 400 (bad request)
 * carrying the message text.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> dataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        String msg = rootCauseMessage(e);
        return body(HttpStatus.CONFLICT, "Data conflict",
                msg != null ? msg : "A record with these details already exists.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> illegalArg(IllegalArgumentException e) {
        log.warn("Illegal argument", e);
        return body(HttpStatus.BAD_REQUEST, "Bad request",
                e.getMessage() != null ? e.getMessage() : "Invalid input.");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> runtime(RuntimeException e) {
        log.warn("Service rejected request: {}", e.getMessage(), e);
        return body(HttpStatus.BAD_REQUEST, "Bad request",
                e.getMessage() != null ? e.getMessage() : "Request failed.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generic(Exception e) {
        log.error("Unhandled error", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Server error",
                e.getMessage() != null ? e.getMessage() : "Unexpected error.");
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String error, String message) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("timestamp", LocalDateTime.now().toString());
        b.put("status", status.value());
        b.put("error", error);
        b.put("message", message);
        return ResponseEntity.status(status).body(b);
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
