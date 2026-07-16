package com.riigiluup.admin;

import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.LegislativeItemImporter;
import com.riigiluup.ingestion.riigikogu.VoteBillLinker;
import com.riigiluup.ingestion.riigikogu.VoteEventImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Ops-only historical backfill — single window, synchronous.
 *
 * <p>Runs {@link VoteEventImporter#runWindow} and {@link LegislativeItemImporter#runWindow}
 * inline over an arbitrary date range. The response holds the connection open until
 * the whole window completes.
 *
 * @deprecated Prefer {@link HistoricalBackfillController} at
 * {@code /api/v1/admin/backfill/full} for multi-window background runs with progress
 * polling and cancellation. The legacy {@code POST /api/v1/admin/backfill} mapping will
 * be removed one release from now; use {@code POST /api/v1/admin/backfill/single}
 * instead for the synchronous single-window mode.
 */
@Deprecated
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/backfill")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminBackfillController {

    public enum Kind { BILLS, VOTES }

    private static final Set<Kind> DEFAULT_KINDS = EnumSet.of(Kind.BILLS, Kind.VOTES);

    private final LegislativeItemImporter legislationImporter;
    private final VoteEventImporter voteImporter;
    private final VoteBillLinker voteBillLinker;

    /**
     * @deprecated call {@link #backfillSingle(String, String, String)} at
     * {@code /api/v1/admin/backfill/single} instead. This mapping keeps working for one
     * release and stamps a {@code Deprecation} HTTP header on responses.
     */
    @Deprecated
    @PostMapping
    public ResponseEntity<BackfillResult> backfill(
            @RequestParam("from") String fromRaw,
            @RequestParam(value = "to", required = false) String toRaw,
            @RequestParam(value = "kinds", required = false) String kindsRaw
    ) {
        ResponseEntity<BackfillResult> response = runSingleWindow(fromRaw, toRaw, kindsRaw);
        // RFC 8594 Deprecation header — the value is a HTTP-date literal placeholder
        // ("true" is also accepted by tooling like sunset-headers).
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(response.getHeaders());
        headers.set("Deprecation", "true");
        headers.set("Link",
                "</api/v1/admin/backfill/single>; rel=\"successor-version\", "
                        + "</api/v1/admin/backfill/full>; rel=\"alternate\"");
        return ResponseEntity.status(response.getStatusCode())
                .headers(headers)
                .body(response.getBody());
    }

    /** Successor mapping for the synchronous single-window backfill. */
    @PostMapping("/single")
    public ResponseEntity<BackfillResult> backfillSingle(
            @RequestParam("from") String fromRaw,
            @RequestParam(value = "to", required = false) String toRaw,
            @RequestParam(value = "kinds", required = false) String kindsRaw
    ) {
        return runSingleWindow(fromRaw, toRaw, kindsRaw);
    }

    private ResponseEntity<BackfillResult> runSingleWindow(
            String fromRaw, String toRaw, String kindsRaw
    ) {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromRaw);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(
                    BackfillResult.error("Invalid 'from' date, expected YYYY-MM-DD: " + fromRaw));
        }
        if (toRaw == null || toRaw.isBlank()) {
            to = LocalDate.now();
        } else {
            try {
                to = LocalDate.parse(toRaw);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body(
                        BackfillResult.error("Invalid 'to' date, expected YYYY-MM-DD: " + toRaw));
            }
        }
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().body(
                    BackfillResult.error("'to' (" + to + ") must be on or after 'from' (" + from + ")"));
        }

        Set<Kind> kinds;
        try {
            kinds = parseKinds(kindsRaw);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BackfillResult.error(e.getMessage()));
        }

        log.info("backfill starting from={} to={} kinds={}", from, to, kinds);
        long start = System.currentTimeMillis();
        Map<String, Long> counts = new LinkedHashMap<>();

        if (kinds.contains(Kind.BILLS)) {
            ImportRunLog run = legislationImporter.runWindow(from, to);
            counts.put("bills", (long) run.getRecordsUpserted());
            log.info("backfill BILLS finished status={} seen={} upserted={}",
                    run.getStatus(), run.getRecordsSeen(), run.getRecordsUpserted());
        }
        if (kinds.contains(Kind.VOTES)) {
            ImportRunLog run = voteImporter.runWindow(from, to);
            counts.put("votes", (long) run.getRecordsUpserted());
            log.info("backfill VOTES finished status={} seen={} upserted={}",
                    run.getStatus(), run.getRecordsSeen(), run.getRecordsUpserted());
        }
        // If both bills and votes came through, connect them via the same linker used daily.
        if (kinds.contains(Kind.BILLS) && kinds.contains(Kind.VOTES)) {
            try {
                voteBillLinker.linkAll();
            } catch (Exception e) {
                log.warn("vote-bill link step failed after backfill: {}", e.toString());
            }
        }

        long windowMs = System.currentTimeMillis() - start;
        log.info("backfill finished in {}ms counts={}", windowMs, counts);
        return ResponseEntity.ok(new BackfillResult(true, windowMs, counts, null));
    }

    private static Set<Kind> parseKinds(String raw) {
        if (raw == null || raw.isBlank()) return DEFAULT_KINDS;
        EnumSet<Kind> out = EnumSet.noneOf(Kind.class);
        for (String token : raw.split(",")) {
            String t = token.trim().toUpperCase(Locale.ROOT);
            if (t.isEmpty()) continue;
            try {
                out.add(Kind.valueOf(t));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Unknown kind '" + t + "'. Allowed: BILLS, VOTES");
            }
        }
        if (out.isEmpty()) return DEFAULT_KINDS;
        return out;
    }

    /** Response shape. `error` is null on success. */
    public record BackfillResult(
            boolean started,
            long windowMs,
            Map<String, Long> counts,
            String error
    ) {
        static BackfillResult error(String message) {
            return new BackfillResult(false, 0L, Map.of(), message);
        }
    }
}
