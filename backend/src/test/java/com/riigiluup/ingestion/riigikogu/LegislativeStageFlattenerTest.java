package com.riigiluup.ingestion.riigikogu;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LegislativeStageFlattenerTest {

    private final LegislativeStageFlattener flat = new LegislativeStageFlattener();

    @Test
    void flattens_readings_and_events_in_chronological_order() {
        List<DraftDetailDto.Reading> readings = List.of(
                new DraftDetailDto.Reading("INITIATION", List.of(
                        new DraftDetailDto.ProceedingEvent("2025-01-13T15:43:59.288", "ALGATATUD"),
                        new DraftDetailDto.ProceedingEvent("2025-01-14T13:41:00.336", "MENETLUSSE_VOETUD")
                )),
                new DraftDetailDto.Reading("ESIMENE_LUGEMINE", List.of(
                        new DraftDetailDto.ProceedingEvent("2025-02-10T10:00:00", "LOPETATUD")
                ))
        );

        List<LegislativeStageFlattener.Flat> out = flat.flatten(readings);

        assertThat(out).hasSize(3);
        assertThat(out.get(0).readingCode()).isEqualTo("INITIATION");
        assertThat(out.get(0).statusCode()).isEqualTo("ALGATATUD");
        // Source sends local Tallinn wall-clock; 2025-01-13 is EET (UTC+2), so 15:43 → 13:43Z.
        assertThat(out.get(0).occurredAt()).isEqualTo(Instant.parse("2025-01-13T13:43:59.288Z"));
        assertThat(out.get(0).sequence()).isZero();
        assertThat(out.get(1).sequence()).isEqualTo(1);
        assertThat(out.get(2).readingCode()).isEqualTo("ESIMENE_LUGEMINE");
        assertThat(out.get(2).sequence()).isEqualTo(2);
    }

    @Test
    void events_with_null_or_unparseable_dates_are_kept_but_sorted_last() {
        List<DraftDetailDto.Reading> readings = List.of(
                new DraftDetailDto.Reading("INITIATION", List.of(
                        new DraftDetailDto.ProceedingEvent(null, "PENDING"),
                        new DraftDetailDto.ProceedingEvent("2025-01-14T13:00:00", "OK")
                ))
        );

        List<LegislativeStageFlattener.Flat> out = flat.flatten(readings);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).statusCode()).isEqualTo("OK");
        assertThat(out.get(1).statusCode()).isEqualTo("PENDING");
        assertThat(out.get(1).occurredAt()).isNull();
    }

    @Test
    void null_or_empty_readings_yields_empty_list() {
        assertThat(flat.flatten(null)).isEmpty();
        assertThat(flat.flatten(List.of())).isEmpty();
    }
}
