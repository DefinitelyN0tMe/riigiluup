package com.riigiluup.vote;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoteChoiceTest {

    @Test
    void maps_all_known_source_codes() {
        assertThat(VoteChoice.fromSourceCode("POOLT")).isEqualTo(VoteChoice.FOR);
        assertThat(VoteChoice.fromSourceCode("VASTU")).isEqualTo(VoteChoice.AGAINST);
        assertThat(VoteChoice.fromSourceCode("ERAPOOLETU")).isEqualTo(VoteChoice.ABSTAINED);
        assertThat(VoteChoice.fromSourceCode("EI_HAALETANUD")).isEqualTo(VoteChoice.DID_NOT_VOTE);
        assertThat(VoteChoice.fromSourceCode("PUUDUB")).isEqualTo(VoteChoice.ABSENT);
        assertThat(VoteChoice.fromSourceCode("KOHAL")).isEqualTo(VoteChoice.PRESENT);
    }

    @Test
    void unknown_and_null_default_to_UNKNOWN() {
        assertThat(VoteChoice.fromSourceCode(null)).isEqualTo(VoteChoice.UNKNOWN);
        assertThat(VoteChoice.fromSourceCode("")).isEqualTo(VoteChoice.UNKNOWN);
        assertThat(VoteChoice.fromSourceCode("SOMETHING_NEW")).isEqualTo(VoteChoice.UNKNOWN);
    }

    @Test
    void source_codes_are_case_insensitive() {
        assertThat(VoteChoice.fromSourceCode("poolt")).isEqualTo(VoteChoice.FOR);
        assertThat(VoteChoice.fromSourceCode("Ei_Haaletanud")).isEqualTo(VoteChoice.DID_NOT_VOTE);
    }
}
