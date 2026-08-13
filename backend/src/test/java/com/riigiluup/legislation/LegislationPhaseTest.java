package com.riigiluup.legislation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegislationPhaseTest {

    @Test
    void maps_common_stage_codes() {
        assertThat(LegislationPhase.fromStageCode("INITIATION")).isEqualTo(LegislationPhase.SUBMITTED);
        // Source spells "taken into proceeding" as MENETLUSSE_VOETUD.
        assertThat(LegislationPhase.fromStageCode("MENETLUSSE_VOETUD")).isEqualTo(LegislationPhase.SUBMITTED);
        assertThat(LegislationPhase.fromStageCode("ESIMENE_LUGEMINE")).isEqualTo(LegislationPhase.IN_READINGS);
        assertThat(LegislationPhase.fromStageCode("TEINE_LUGEMINE")).isEqualTo(LegislationPhase.IN_READINGS);
        assertThat(LegislationPhase.fromStageCode("KOLMAS_LUGEMINE")).isEqualTo(LegislationPhase.IN_READINGS);
        assertThat(LegislationPhase.fromStageCode("VASTU_VOETUD")).isEqualTo(LegislationPhase.ADOPTED);
        // Source spells "rejected" TAGASI_LYKATUD (Y), not TAGASI_LUKATUD (U).
        assertThat(LegislationPhase.fromStageCode("TAGASI_LYKATUD")).isEqualTo(LegislationPhase.REJECTED);
        assertThat(LegislationPhase.fromStageCode("LOPETATUD")).isEqualTo(LegislationPhase.WITHDRAWN);
        assertThat(LegislationPhase.fromStageCode("TAGASTATUD")).isEqualTo(LegislationPhase.WITHDRAWN);
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
