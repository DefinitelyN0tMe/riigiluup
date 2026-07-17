package com.riigiluup.api;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

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
}
