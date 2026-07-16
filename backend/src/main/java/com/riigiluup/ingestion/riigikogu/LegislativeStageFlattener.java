package com.riigiluup.ingestion.riigikogu;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class LegislativeStageFlattener {

    public List<Flat> flatten(List<DraftDetailDto.Reading> readings) {
        if (readings == null || readings.isEmpty()) return List.of();
        List<Flat> raw = new ArrayList<>();
        for (DraftDetailDto.Reading r : readings) {
            if (r.proceedingEvents() == null) continue;
            for (DraftDetailDto.ProceedingEvent e : r.proceedingEvents()) {
                raw.add(new Flat(r.readingCode(), e.status(), parseTs(e.date()), 0));
            }
        }
        raw.sort(Comparator.comparing(Flat::occurredAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        List<Flat> withSequence = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            Flat f = raw.get(i);
            withSequence.add(new Flat(f.readingCode(), f.statusCode(), f.occurredAt(), i));
        }
        return withSequence;
    }

    // Riigikogu emits offset-less local Estonian wall-clock; interpret in the source zone.
    private static final ZoneId SOURCE_ZONE = ZoneId.of("Europe/Tallinn");

    private static Instant parseTs(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s).atZone(SOURCE_ZONE).toInstant(); }
        catch (Exception e) { return null; }
    }

    public record Flat(String readingCode, String statusCode, Instant occurredAt, int sequence) {}
}
