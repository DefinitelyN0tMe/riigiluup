package com.riigiluup.ingestion.riigiteataja;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * Riigi Teataja search — the SPA's own endpoint (no documented public REST API exists;
 * the official open data is 40+ GB of yearly XML dumps, overkill for linking). The
 * contract is therefore unofficial: callers must treat failures/shape changes as
 * "no link" and degrade gracefully, never hard-fail a pipeline on RT.
 */
@Slf4j
@Component
public class RiigiTeatajaClient {

    private static final ZoneId TALLINN = ZoneId.of("Europe/Tallinn");
    private static final DateTimeFormatter OFFSET_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final RestClient rest;

    public RiigiTeatajaClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.rest = RestClient.builder()
                .baseUrl("https://www.riigiteataja.ee")
                .defaultHeader(HttpHeaders.USER_AGENT, "riigiluup/0.0.1 (riigiluup@gmail.com)")
                .requestFactory(factory)
                .build();
    }

    /**
     * Finds the RT act id for an act published on the given date with the given title.
     * Returns empty unless the search yields EXACTLY one hit — an ambiguous day/title
     * combination must never guess.
     */
    public Optional<Long> findActId(String title, LocalDate publicationDate) {
        throttle();
        String day = publicationDate.atStartOfDay(TALLINN).format(OFFSET_FMT);
        Map<String, Object> body = Map.of(
                "general", Map.of(
                        "searchInTitle", true,
                        "searchInText", false,
                        "searchText", title,
                        "logicalOperator", "AND",
                        "morphSearch", false),
                "precise", Map.of(
                        "publicationDateStart", day,
                        "publicationDateEnd", day));
        try {
            JsonNode response = rest.post()
                    .uri("/public-api/api/v1/otsing/alg-tekst")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.path("koik").asInt(-1) != 1) return Optional.empty();
            long id = response.path("results").path(0).path("id").asLong(0);
            return id > 0 ? Optional.of(id) : Optional.empty();
        } catch (Exception e) {
            log.warn("RT search failed for '{}' @ {}: {}", title, publicationDate, e.toString());
            return Optional.empty();
        }
    }

    private long lastRequestNanos = -1L;
    private static final long MIN_INTERVAL_NANOS = 1_100_000_000L;

    /** RT's rate limits are undocumented — stay at ~0.9 rps like the Riigikogu client. */
    private synchronized void throttle() {
        long now = System.nanoTime();
        if (lastRequestNanos >= 0) {
            long sleepNanos = MIN_INTERVAL_NANOS - (now - lastRequestNanos);
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        lastRequestNanos = System.nanoTime();
    }
}
