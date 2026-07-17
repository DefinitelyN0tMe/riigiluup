package com.riigiluup.initiative;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InitiativeFunnelMathTest {

    /**
     * The funnel is NOT monotonic and must not be "fixed" to look like one: 8 of the 201
     * initiatives actually sent to parliament never passed the 1000-signature step (3 have no
     * count at all, 5 have 13–200). The source does not explain why; we report, not guess.
     */
    @Test
    void keepsLaterStepLargerThanEarlierWhenTheSourceSaysSo() {
        List<InitiativeDto.FunnelStep> steps = InitiativeService.buildFunnel(
                730, 629, 216, 201, 188, 5);

        assertThat(steps).extracting(InitiativeDto.FunnelStep::count)
                .containsExactly(730L, 629L, 216L, 201L, 188L, 5L);
        assertThat(steps.get(3).count()).isGreaterThan(0L);
        assertThat(steps).extracting(InitiativeDto.FunnelStep::key)
                .containsExactly("targeted", "signing", "threshold", "sent", "decided", "draftAct");
    }

    /** Share is always against step 1, never against the previous step — one honest baseline. */
    @Test
    void sharesAreRelativeToTheFirstStep() {
        List<InitiativeDto.FunnelStep> steps = InitiativeService.buildFunnel(
                1000, 500, 250, 200, 100, 10);

        assertThat(steps.get(0).shareOfTargeted()).isEqualTo(100.0);
        assertThat(steps.get(1).shareOfTargeted()).isEqualTo(50.0);
        assertThat(steps.get(5).shareOfTargeted()).isEqualTo(1.0);
    }

    @Test
    void emptyDatasetDoesNotDivideByZero() {
        List<InitiativeDto.FunnelStep> steps = InitiativeService.buildFunnel(0, 0, 0, 0, 0, 0);

        assertThat(steps).hasSize(6);
        assertThat(steps).allSatisfy(s -> assertThat(s.shareOfTargeted()).isNull());
    }
}
