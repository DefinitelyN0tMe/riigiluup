package com.politico.legislation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegislationPhaseTest {

    @Test
    void maps_common_stage_codes() {
        assertThat(LegislationPhase.fromStageCode("INITIATION")).isEqualTo(LegislationPhase.SUBMITTED);
        assertThat(LegislationPhase.fromStageCode("ESIMENE_LUGEMINE")).isEqualTo(LegislationPhase.IN_READINGS);
        assertThat(LegislationPhase.fromStageCode("TEINE_LUGEMINE")).isEqualTo(LegislationPhase.IN_READINGS);
        assertThat(LegislationPhase.fromStageCode("KOLMAS_LUGEMINE")).isEqualTo(LegislationPhase.IN_READINGS);
        assertThat(LegislationPhase.fromStageCode("VASTU_VOETUD")).isEqualTo(LegislationPhase.ADOPTED);
        assertThat(LegislationPhase.fromStageCode("TAGASI_LUKATUD")).isEqualTo(LegislationPhase.REJECTED);
        assertThat(LegislationPhase.fromStageCode("LOPETATUD")).isEqualTo(LegislationPhase.WITHDRAWN);
    }

    @Test
    void unknown_or_null_falls_through_to_OTHER() {
        assertThat(LegislationPhase.fromStageCode(null)).isEqualTo(LegislationPhase.OTHER);
        assertThat(LegislationPhase.fromStageCode("")).isEqualTo(LegislationPhase.OTHER);
        assertThat(LegislationPhase.fromStageCode("NEW_STAGE_2027")).isEqualTo(LegislationPhase.OTHER);
    }

    @Test
    void source_code_is_case_insensitive() {
        assertThat(LegislationPhase.fromStageCode("vastu_voetud")).isEqualTo(LegislationPhase.ADOPTED);
    }
}
