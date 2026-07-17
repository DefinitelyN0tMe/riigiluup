package com.riigiluup.initiative;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InitiativeCommitteeTest {

    /** All 11 slugs the source emits map 1:1 onto the 11 active standing committees. */
    @Test
    void mapsAllElevenSlugsToCommitteeNames() {
        assertThat(InitiativeCommittee.fromSlug("constitutional"))
                .get().extracting(InitiativeCommittee::committeeName)
                .isEqualTo("Põhiseaduskomisjon");
        assertThat(InitiativeCommittee.fromSlug("cultural-affairs"))
                .get().extracting(InitiativeCommittee::committeeName)
                .isEqualTo("Kultuurikomisjon");
        assertThat(InitiativeCommittee.fromSlug("economic-affairs"))
                .get().extracting(InitiativeCommittee::committeeName)
                .isEqualTo("Majanduskomisjon");
        assertThat(InitiativeCommittee.fromSlug("environment"))
                .get().extracting(InitiativeCommittee::committeeName)
                .isEqualTo("Keskkonnakomisjon");
        assertThat(InitiativeCommittee.fromSlug("eu-affairs"))
                .get().extracting(InitiativeCommittee::committeeName)
                .isEqualTo("Euroopa Liidu asjade komisjon");
        assertThat(InitiativeCommittee.fromSlug("finance"))
                .get().extracting(InitiativeCommittee::committeeName)
                .isEqualTo("Rahanduskomisjon");
        assertThat(InitiativeCommittee.fromSlug("foreign-affairs"))
                .get().extracting(InitiativeCommittee::committeeName)
                .isEqualTo("Väliskomisjon");
        assertThat(InitiativeCommittee.fromSlug("legal-affairs"))
                .get().extracting(InitiativeCommittee::committeeName)
                .isEqualTo("Õiguskomisjon");
        assertThat(InitiativeCommittee.fromSlug("national-defence"))
                .get().extracting(InitiativeCommittee::committeeName)
                .isEqualTo("Riigikaitsekomisjon");
        assertThat(InitiativeCommittee.fromSlug("rural-affairs"))
                .get().extracting(InitiativeCommittee::committeeName)
                .isEqualTo("Maaelukomisjon");
        assertThat(InitiativeCommittee.fromSlug("social-affairs"))
                .get().extracting(InitiativeCommittee::committeeName)
                .isEqualTo("Sotsiaalkomisjon");
        assertThat(InitiativeCommittee.values()).hasSize(11);
    }

    /**
     * Unlike phase/decision, an unknown committee slug degrades gracefully: it is a label, not
     * a metric input. The importer keeps the raw slug and leaves group_id NULL.
     */
    @Test
    void unknownSlugIsEmptyNotAnError() {
        assertThat(InitiativeCommittee.fromSlug("space-affairs")).isEmpty();
        assertThat(InitiativeCommittee.fromSlug(null)).isEmpty();
        assertThat(InitiativeCommittee.fromSlug("")).isEmpty();
    }

    @Test
    void slugRoundTrips() {
        Optional<InitiativeCommittee> c = InitiativeCommittee.fromSlug("eu-affairs");
        assertThat(c).get().extracting(InitiativeCommittee::slug).isEqualTo("eu-affairs");
    }
}
