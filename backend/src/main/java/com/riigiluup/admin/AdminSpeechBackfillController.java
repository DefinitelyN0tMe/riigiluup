package com.riigiluup.admin;

import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.SpeechImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ops-only speech backfill — synchronous over a date range. The importer chunks the range
 * into ~2-week verbatim requests internally, so a full-year backfill is only ~26 HTTP calls;
 * the response holds the connection until the window completes.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/backfill/speeches")
@RequiredArgsConstructor
public class AdminSpeechBackfillController {

    private final SpeechImporter importer;

    @PostMapping
    public ResponseEntity<Map<String, Object>> run(
            @RequestParam("from") String fromRaw,
            @RequestParam(value = "to", required = false) String toRaw
    ) {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromRaw);
            to = toRaw == null ? LocalDate.now() : LocalDate.parse(toRaw);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_date", "message", e.getMessage()));
        }
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_range", "message", "to is before from"));
        }
        log.info("admin speech backfill {}..{}", from, to);
        ImportRunLog run = importer.runWindow(from, to);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", run.getStatus());
        body.put("recordsSeen", run.getRecordsSeen());
        body.put("recordsUpserted", run.getRecordsUpserted());
        body.put("errorMessage", run.getErrorMessage());
        return ResponseEntity.ok(body);
    }
}
