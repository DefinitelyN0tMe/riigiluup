package com.riigiluup.initiative;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InitiativeEnumsTest {

    @Test
    void mapsEveryPhaseSlugTheSourceEmits() {
        assertThat(InitiativePhase.fromSlug("edit")).isEqualTo(InitiativePhase.EDIT);
        assertThat(InitiativePhase.fromSlug("sign")).isEqualTo(InitiativePhase.SIGN);
        assertThat(InitiativePhase.fromSlug("parliament")).isEqualTo(InitiativePhase.PARLIAMENT);
        assertThat(InitiativePhase.fromSlug("government")).isEqualTo(InitiativePhase.GOVERNMENT);
        assertThat(InitiativePhase.fromSlug("done")).isEqualTo(InitiativePhase.DONE);
    }

    @Test
    void mapsEveryDecisionSlugTheSourceEmits() {
        assertThat(ParliamentDecision.fromSlug("return")).isEqualTo(ParliamentDecision.RETURN);
        assertThat(ParliamentDecision.fromSlug("reject")).isEqualTo(ParliamentDecision.REJECT);
        assertThat(ParliamentDecision.fromSlug("solve-differently"))
                .isEqualTo(ParliamentDecision.SOLVE_DIFFERENTLY);
        assertThat(ParliamentDecision.fromSlug("forward")).isEqualTo(ParliamentDecision.FORWARD);
        assertThat(ParliamentDecision.fromSlug("forward-to-government"))
                .isEqualTo(ParliamentDecision.FORWARD_TO_GOVERNMENT);
        assertThat(ParliamentDecision.fromSlug("draft-act-or-national-matter"))
                .isEqualTo(ParliamentDecision.DRAFT_ACT_OR_NATIONAL_MATTER);
    }

    @Test
    void blankIsNullNotAnError() {
        assertThat(InitiativePhase.fromSlug(null)).isNull();
        assertThat(InitiativePhase.fromSlug("")).isNull();
        assertThat(ParliamentDecision.fromSlug("")).isNull();
    }

    /** A new source value must break the import loudly, never land in the DB as junk. */
    @Test
    void unknownSlugFailsLoudly() {
        assertThatThrownBy(() -> InitiativePhase.fromSlug("teleported"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teleported");
        assertThatThrownBy(() -> ParliamentDecision.fromSlug("ignored-it"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ignored-it");
    }

    @Test
    void slugRoundTripsForTheDatabaseColumn() {
        assertThat(ParliamentDecision.SOLVE_DIFFERENTLY.slug()).isEqualTo("solve-differently");
        assertThat(InitiativePhase.DONE.slug()).isEqualTo("done");
    }
}
