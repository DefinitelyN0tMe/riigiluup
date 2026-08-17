package com.riigiluup.api;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single JSON error contract for the public API: {status, error, message, path, timestamp}.
 * The SPA renders translated messages from it. Bad client input → 400, unknown routes → 404,
 * constraint collisions → 409; anything genuinely unexpected → a logged 500 whose body carries
 * no internal detail.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Map<String, Object>> badRequest(Exception ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "bad_request", sanitizeBadRequest(ex), req);
    }

    /**
     * Keep useful hand-written validation messages, but never echo JVM-internal detail:
     * a bad enum param (e.g. VoteEventType.valueOf("FOO")) throws "No enum constant
     * com.riigiluup.vote.VoteEventType.FOO", and a type-mismatch names the target class.
     */
    private static String sanitizeBadRequest(Exception ex) {
        if (ex instanceof MethodArgumentTypeMismatchException mm) {
            return "Invalid value for parameter '" + mm.getName() + "'";
        }
        String msg = ex.getMessage();
        if (msg == null || msg.startsWith("No enum constant")) {
            return "Invalid value for a request parameter";
        }
        return msg;
    }

    /** Bean-validation failures on @RequestBody DTOs — condensed to field: message pairs. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> beanValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return body(HttpStatus.BAD_REQUEST, "bad_request",
                message.isEmpty() ? "Invalid request" : message, req);
    }

    /** Bean-validation failures on @RequestParam/@PathVariable (method-level @Validated). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> constraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return body(HttpStatus.BAD_REQUEST, "bad_request",
                message.isEmpty() ? "Invalid request" : message, req);
    }

    /** Unknown routes/static resources get the same JSON envelope instead of the whitelabel body. */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Map<String, Object>> notFound(Exception ex, HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, "not_found", "Resource not found", req);
    }

    /** Unique/FK violations surface as a conflict, not a 500 — details stay in the log. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> conflict(
            DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("data integrity violation on {}: {}", req.getRequestURI(), ex.getMessage());
        return body(HttpStatus.CONFLICT, "conflict",
                "Request conflicts with existing data", req);
    }

    /** Catch-all: log the full stack, leak nothing into the response body. */
    /**
     * Security exceptions must reach Spring Security's ExceptionTranslationFilter (401/403,
     * OAuth redirects) — rethrow so the catch-all below can never turn them into a 500.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void rethrowSecurity(Exception ex) throws Exception {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> internalError(Exception ex, HttpServletRequest req) {
        // Spring MVC exceptions (405, 406, 415, …) carry their own status — keep it instead
        // of downgrading them to a 500.
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.resolve(er.getStatusCode().value());
            if (status != null && !status.is5xxServerError()) {
                return body(status, status.name().toLowerCase(Locale.ROOT),
                        status.getReasonPhrase(), req);
            }
        }
        log.error("unhandled exception on {}", req.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                "Internal server error", req);
    }

    private static ResponseEntity<Map<String, Object>> body(
            HttpStatus status, String error, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", error,
                "message", message,
                "path", req.getRequestURI(),
                "timestamp", Instant.now().toString()
        ));
    }
}
