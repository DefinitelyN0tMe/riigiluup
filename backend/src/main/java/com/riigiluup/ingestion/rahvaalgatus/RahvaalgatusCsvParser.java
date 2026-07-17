package com.riigiluup.ingestion.rahvaalgatus;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Parses the rahvaalgatus.ee CSV export. Pure — no HTTP, no Spring — so the source's real
 * quirks are covered by string fixtures instead of a live call.
 *
 * <p>Quirks handled here, all verified against the live file on 2026-07-17:
 * <ul>
 *   <li>real line breaks inside quoted {@code title} / {@code parliament_committees} values
 *       (1292 physical lines for 1141 records);</li>
 *   <li>several committees packed into one field, newline-separated;</li>
 *   <li>blank numbers and dates, which must stay null rather than become 0 / epoch.</li>
 * </ul>
 *
 * <p>Rows are read into a map keyed by header name, so a new source column is ignored and a
 * removed one yields null instead of breaking the run.
 */
public final class RahvaalgatusCsvParser {

    private static final CsvMapper MAPPER = CsvMapper.builder().build();
    private static final CsvSchema SCHEMA = CsvSchema.emptySchema().withHeader();

    private RahvaalgatusCsvParser() {
    }

    /** One initiative, source columns already typed. Committees are split, not raw. */
    public record Row(
            String externalId,
            String uuid,
            String title,
            String authors,
            String destination,
            String phase,
            Instant publishedAt,
            Instant signingStartedAt,
            Instant signingEndsAt,
            Integer signatureCount,
            Instant lastSignedAt,
            Instant sentToParliamentAt,
            List<String> committees,
            String parliamentDecision,
            Instant finishedInParliamentAt,
            Instant sentToGovernmentAt,
            Instant finishedInGovernmentAt
    ) {
    }

    public static List<Row> parse(String csv) {
        try (MappingIterator<Map<String, String>> it =
                     MAPPER.readerForMapOf(String.class).with(SCHEMA).readValues(csv)) {
            List<Row> rows = new ArrayList<>();
            while (it.hasNext()) {
                rows.add(toRow(it.next()));
            }
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse rahvaalgatus CSV", e);
        }
    }

    private static Row toRow(Map<String, String> c) {
        return new Row(
                text(c, "id"),
                text(c, "uuid"),
                text(c, "title"),
                text(c, "authors"),
                text(c, "destination"),
                text(c, "phase"),
                instant(c, "published_at"),
                instant(c, "signing_started_at"),
                instant(c, "signing_ends_at"),
                integer(c, "signature_count"),
                instant(c, "last_signed_at"),
                instant(c, "sent_to_parliament_at"),
                committees(text(c, "parliament_committees")),
                text(c, "parliament_decision"),
                instant(c, "finished_in_parliament_at"),
                instant(c, "sent_to_government_at"),
                instant(c, "finished_in_government_at")
        );
    }

    private static String text(Map<String, String> c, String column) {
        String v = c.get(column);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static Integer integer(Map<String, String> c, String column) {
        String v = text(c, column);
        return v == null ? null : Integer.valueOf(v);
    }

    private static Instant instant(Map<String, String> c, String column) {
        String v = text(c, column);
        return v == null ? null : Instant.parse(v);
    }

    /** Newline-separated inside a single quoted field; order preserved, duplicates dropped. */
    static List<String> committees(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return List.copyOf(new LinkedHashSet<>(
                Arrays.stream(raw.split("\\R"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()));
    }
}
