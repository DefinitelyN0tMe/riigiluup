package com.riigiluup.ingestion.rahvaalgatus;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * rahvaalgatus.ee CSV export (SA Eesti Koostöö Kogu).
 *
 * <p>Verified 2026-07-17: GET /initiatives with Accept: text/csv returns EVERY initiative in
 * every phase — 1141 records, ~284 KB, one request, no pagination, no auth. The documented
 * JSON API is poorer (no committee, no decision), and the '?phase=done' filter the source
 * spike recorded only narrows the set — the funnel needs all phases, so we do not send it.
 *
 * <p>NB: the path has NO '/api' prefix. https://rahvaalgatus.ee/api/initiatives returns 404.
 */
@Component
public class RahvaalgatusClient {

    public static final String SOURCE_NAME = "rahvaalgatus";
    public static final String CSV_URL = "https://rahvaalgatus.ee/initiatives";

    private final RestClient rest;

    public RahvaalgatusClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(30_000);
        this.rest = RestClient.builder()
                .baseUrl("https://rahvaalgatus.ee")
                .defaultHeader(HttpHeaders.USER_AGENT, "riigiluup/0.0.1 (riigiluup@gmail.com)")
                .defaultHeader(HttpHeaders.ACCEPT, "text/csv")
                .requestFactory(factory)
                .build();
    }

    public String sourceName() {
        return SOURCE_NAME;
    }

    /** @return the raw CSV body; caller parses. */
    public String fetchInitiativesCsv() {
        return rest.get().uri("/initiatives").retrieve().body(String.class);
    }
}
