package com.politico.admin;

import com.politico.ingestion.riigikogu.ImportRunLog;
import com.politico.ingestion.riigikogu.LegislativeItemImporter;
import com.politico.ingestion.riigikogu.VoteBillLinker;
import com.politico.ingestion.riigikogu.VoteEventImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Ops-only historical backfill.
 *
 * <p>Runs the existing {@link VoteEventImporter#runWindow} and
 * {@link LegislativeItemImporter#runWindow} inline over an arbitrary date range so
 * ops can seed the DB with a multi-year window (e.g. 2022..today) without a code change
 * or waiting for the daily scheduled job. The response holds the connection open until
 * the whole window completes — this is intentional. For very large ranges the caller
 * is expected to send several smaller requests.
 */
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

    @PostMapping
    public ResponseEntity<BackfillResult> backfill(
            @RequestParam("from") String fromRaw,
            @RequestParam(value = "to", required = false) String toRaw,
            @RequestParam(value = "kinds", required = false) String kindsRaw
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
