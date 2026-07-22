package com.riigiluup.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Locks in the HTTP boundary for bad client input on the public API: an unrecognised
 * rahvaalgatus enum slug (e.g. {@code ?phase=bogus} on {@code /api/v1/initiatives}) must
 * surface as 400, not a bare 500 — while {@link com.riigiluup.initiative.InitiativePhase}
 * and {@link com.riigiluup.initiative.ParliamentDecision} keep throwing on an unknown slug
 * so a new source value still fails the import loudly (see InitiativeEnumsTest).
 */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void mapsIllegalArgumentExceptionTo400() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/initiatives");

        ResponseEntity<Map<String, Object>> response = handler.badRequest(
                new IllegalArgumentException("Unknown rahvaalgatus phase: 'bogus'"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("status", 400)
                .containsEntry("error", "bad_request")
                .containsEntry("message", "Unknown rahvaalgatus phase: 'bogus'")
                .containsEntry("path", "/api/v1/initiatives");
    }

    @Test
    void fallsBackToAGenericMessageWhenTheExceptionHasNone() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/v1/initiatives");

        ResponseEntity<Map<String, Object>> response =
                handler.badRequest(new IllegalArgumentException(), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Invalid request");
    }

    @Test
    void mapsUnknownRouteTo404WithTheSameJsonEnvelope() {
        HttpServletRequest req = request("/api/v1/nope");

        ResponseEntity<Map<String, Object>> response = handler.notFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/v1/nope"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .containsEntry("status", 404)
                .containsEntry("error", "not_found")
                .containsEntry("message", "Resource not found")
                .containsEntry("path", "/api/v1/nope");
    }

    @Test
    void mapsDataIntegrityViolationTo409WithoutLeakingTheConstraintDetail() {
        HttpServletRequest req = request("/api/v1/admin/affiliations");

        ResponseEntity<Map<String, Object>> response = handler.conflict(
                new DataIntegrityViolationException("duplicate key ux_mp_ext_aff_natural"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .containsEntry("status", 409)
                .containsEntry("error", "conflict")
                .containsEntry("path", "/api/v1/admin/affiliations");
        assertThat((String) response.getBody().get("message")).doesNotContain("ux_mp_ext_aff_natural");
    }

    @Test
    void condensesBeanValidationErrorsIntoA400FieldMessage() {
        HttpServletRequest req = request("/api/v1/admin/affiliations");
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "dto");
        binding.addError(new FieldError("dto", "organization", "must not be blank"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(binding);

        ResponseEntity<Map<String, Object>> response = handler.beanValidation(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("error", "bad_request")
                .containsEntry("message", "organization: must not be blank");
    }

    @Test
    void mapsConstraintViolationTo400WithAFallbackMessage() {
        HttpServletRequest req = request("/api/v1/politicians");

        ResponseEntity<Map<String, Object>> response = handler.constraintViolation(
                new ConstraintViolationException("ignored", Set.of()), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("error", "bad_request")
                .containsEntry("message", "Invalid request");
    }

    @Test
    void keepsTheFrameworkStatusForSelfDescribingMvcExceptions() {
        HttpServletRequest req = request("/api/v1/politicians");

        ResponseEntity<Map<String, Object>> response = handler.internalError(
                new org.springframework.web.HttpRequestMethodNotSupportedException("DELETE"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody())
                .containsEntry("status", 405)
                .containsEntry("error", "method_not_allowed");
    }

    @Test
    void mapsUnexpectedExceptionTo500WithoutLeakingInternals() {
        HttpServletRequest req = request("/api/v1/politicians");

        ResponseEntity<Map<String, Object>> response = handler.internalError(
                new RuntimeException("jdbc url jdbc:postgresql://db:5432 refused"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .containsEntry("status", 500)
                .containsEntry("error", "internal_error")
                .containsEntry("message", "Internal server error")
                .containsEntry("path", "/api/v1/politicians");
        assertThat((String) response.getBody().get("message")).doesNotContain("jdbc");
    }

    private static HttpServletRequest request(String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(uri);
        return req;
    }
}
