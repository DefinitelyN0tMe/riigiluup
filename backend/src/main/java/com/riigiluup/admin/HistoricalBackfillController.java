package com.riigiluup.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Ops endpoints for the long-running, resumable historical backfill.
 *
 * <p>Distinct from {@link AdminBackfillController}: that one runs a single window inline
 * (fine for a few days). This one accepts multi-year ranges and runs on a background
 * thread with progress persisted to the {@code backfill_run} table.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/backfill/full")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HistoricalBackfillController {

    private final HistoricalBackfillOrchestrator orchestrator;

    @PostMapping
    public ResponseEntity<Map<String, Object>> start(
            @RequestParam("from") String fromRaw,
            @RequestParam(value = "to", required = false) String toRaw,
            @RequestParam(value = "kinds", required = false) String kindsRaw
    ) {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromRaw);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(error(
                    "Invalid 'from' date, expected YYYY-MM-DD: " + fromRaw));
        }
        if (toRaw == null || toRaw.isBlank()) {
            to = LocalDate.now();
        } else {
            try {
                to = LocalDate.parse(toRaw);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body(error(
                        "Invalid 'to' date, expected YYYY-MM-DD: " + toRaw));
            }
        }
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().body(error(
                    "'to' (" + to + ") must be on or after 'from' (" + from + ")"));
        }

        Set<String> kinds;
        try {
            kinds = parseKinds(kindsRaw);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }

        Optional<BackfillRun> started = orchestrator.startAsync(from, to, kinds);
        if (started.isEmpty()) {
            Map<String, Object> body = error(
                    "Another backfill run is already RUNNING — cancel it before starting a new one");
            return ResponseEntity.status(409).body(body);
        }
        BackfillRun run = started.get();
        log.info("historical backfill accepted runId={} from={} to={} kinds={}",
                run.getId(), from, to, kinds);
        return ResponseEntity.accepted().body(toJson(run));
    }

    @GetMapping("/{runId}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable("runId") UUID runId) {
        return orchestrator.getProgress(runId)
                .map(r -> ResponseEntity.ok(toJson(r)))
                .orElseGet(() -> ResponseEntity.status(404).body(error("run not found: " + runId)));
    }

    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> latest() {
        return orchestrator.getLatest()
                .map(r -> ResponseEntity.ok(toJson(r)))
                .orElseGet(() -> ResponseEntity.status(404).body(error("no backfill runs yet")));
    }

    @PostMapping("/{runId}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable("runId") UUID runId) {
        return orchestrator.cancel(runId)
                .map(r -> ResponseEntity.ok(toJson(r)))
                .orElseGet(() -> ResponseEntity.status(404).body(error("run not found: " + runId)));
    }

    // ------------------------------------------------------------------------

    private static Set<String> parseKinds(String raw) {
        if (raw == null || raw.isBlank()) return Set.of("BILLS", "VOTES");
        Set<String> out = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String t = token.trim().toUpperCase(Locale.ROOT);
            if (t.isEmpty()) continue;
            if (!t.equals("BILLS") && !t.equals("VOTES")) {
                throw new IllegalArgumentException(
                        "Unknown kind '" + t + "'. Allowed: BILLS, VOTES");
            }
            out.add(t);
        }
        if (out.isEmpty()) return Set.of("BILLS", "VOTES");
        return out;
    }

    private static Map<String, Object> toJson(BackfillRun r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("runId", r.getId());
        m.put("status", r.getStatus());
        m.put("fromDate", r.getFromDate());
        m.put("toDate", r.getToDate());
        m.put("currentWindowStart", r.getCurrentWindowStart());
        m.put("kinds", r.getKinds());
        m.put("startedAt", nullSafe(r.getStartedAt()));
        m.put("endedAt", nullSafe(r.getEndedAt()));
        m.put("billsImported", r.getBillsImported());
        m.put("votesImported", r.getVotesImported());
        m.put("windowsCompleted", r.getWindowsCompleted());
        m.put("windowsTotal", r.getWindowsTotal());
        m.put("errorMessage", r.getErrorMessage());
        return m;
    }

    private static Object nullSafe(Instant i) {
        return i == null ? null : i.toString();
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", message);
        return m;
    }
}
