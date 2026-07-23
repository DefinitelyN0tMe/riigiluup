package com.riigiluup.analytics.umami;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

/**
 * Thin read-only client for the Umami REST API (v2), talking to the internal
 * {@code umami:3000} service. Authenticates once with the admin credentials and caches
 * the bearer token, re-logging in transparently on a 401 (tokens can expire). All calls
 * are GETs against a single website id; responses are returned as raw {@link JsonNode}
 * and shaped into the admin DTO by the controller.
 *
 * <p>URIs are built with the {@link UriBuilder} lambda form so query values (notably the
 * {@code timezone} "Europe/Tallinn") are encoded exactly once — pre-encoding the string
 * ourselves led to double-encoding ("%252F") and an "Invalid timezone" 400 from Umami.
 */
@Component
public class UmamiClient {

    private final UmamiProperties props;
    private final RestClient rest;
    private volatile String token;

    public UmamiClient(UmamiProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);
        this.rest = RestClient.builder()
                .baseUrl(props.baseUrl() == null || props.baseUrl().isBlank()
                        ? "http://umami:3000" : props.baseUrl())
                .requestFactory(factory)
                .build();
    }

    /** False when password/websiteId are unset (dev/local) — caller returns "not configured". */
    public boolean configured() {
        return notBlank(props.password()) && notBlank(props.websiteId());
    }

    private synchronized String login() {
        JsonNode res = rest.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", props.username(), "password", props.password()))
                .retrieve()
                .body(JsonNode.class);
        if (res == null || res.get("token") == null) {
            throw new IllegalStateException("Umami login returned no token");
        }
        this.token = res.get("token").asText();
        return token;
    }

    private JsonNode get(Function<UriBuilder, URI> uriFn) {
        if (token == null) login();
        try {
            return doGet(uriFn);
        } catch (HttpClientErrorException.Unauthorized e) {
            login();            // token expired — one retry with a fresh token
            return doGet(uriFn);
        }
    }

    private JsonNode doGet(Function<UriBuilder, URI> uriFn) {
        return rest.get().uri(uriFn)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode stats(long startAt, long endAt) {
        return get(b -> b.path("/api/websites/{id}/stats")
                .queryParam("startAt", startAt).queryParam("endAt", endAt)
                .build(props.websiteId()));
    }

    public JsonNode pageviews(long startAt, long endAt, String unit, String timezone) {
        return get(b -> b.path("/api/websites/{id}/pageviews")
                .queryParam("startAt", startAt).queryParam("endAt", endAt)
                .queryParam("unit", unit).queryParam("timezone", timezone)
                .build(props.websiteId()));
    }

    public JsonNode metrics(long startAt, long endAt, String type, int limit) {
        return get(b -> b.path("/api/websites/{id}/metrics")
                .queryParam("startAt", startAt).queryParam("endAt", endAt)
                .queryParam("type", type).queryParam("limit", limit)
                .build(props.websiteId()));
    }

    /** Visitors seen in the last few minutes. Handles both the array and object shapes. */
    public int activeVisitors() {
        JsonNode n = get(b -> b.path("/api/websites/{id}/active").build(props.websiteId()));
        if (n == null) return 0;
        if (n.isArray()) return n.isEmpty() ? 0 : n.get(0).path("x").asInt(0);
        return n.path("visitors").asInt(0);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
