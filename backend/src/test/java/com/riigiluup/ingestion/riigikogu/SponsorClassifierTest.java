package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.legislation.SponsorKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SponsorClassifierTest {

    private final SponsorClassifier c = new SponsorClassifier();

    @Test
    void maps_source_type_labels() {
        assertThat(c.classify(new DraftDetailDto.Initiator("m-1", "Jaak Aab", "plenarymember", true)))
                .isEqualTo(SponsorKind.PLENARY_MEMBER);
        assertThat(c.classify(new DraftDetailDto.Initiator("f-1", "Reformierakonna fraktsioon", "usergroup", true)))
                .isEqualTo(SponsorKind.FACTION);
        assertThat(c.classify(new DraftDetailDto.Initiator("g-1", "Vabariigi Valitsus", "classifier", true)))
                .isEqualTo(SponsorKind.ORGAN);
    }

    @Test
    void faction_detected_by_name_suffix_when_type_is_generic_usergroup() {
        assertThat(c.classify(new DraftDetailDto.Initiator("k-1", "Õiguskomisjon", "usergroup", true)))
                .isEqualTo(SponsorKind.COMMITTEE);
    }

    @Test
    void unknown_type_and_name_falls_through_to_OTHER() {
        assertThat(c.classify(new DraftDetailDto.Initiator("x-1", "Something Else", "brand-new", true)))
                .isEqualTo(SponsorKind.OTHER);
        assertThat(c.classify(new DraftDetailDto.Initiator(null, null, null, null)))
                .isEqualTo(SponsorKind.OTHER);
    }
}
