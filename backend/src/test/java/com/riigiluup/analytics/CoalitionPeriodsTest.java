package com.riigiluup.analytics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoalitionPeriodsTest {

    @Test
    void parsesPeriodsInDateOrderWithLowercasedNames() {
        List<AnalyticsService.CoalitionPeriod> p = AnalyticsService.parseCoalitionPeriods(
                " 2025-03-11=Reform, Eesti 200 ;2023-04-10=reform,eesti 200,sotsiaal; ");
        assertThat(p).hasSize(2);
        assertThat(p.get(0).from()).isEqualTo(LocalDate.of(2023, 4, 10));
        assertThat(p.get(0).names()).containsExactly("reform", "eesti 200", "sotsiaal");
        assertThat(p.get(1).from()).isEqualTo(LocalDate.of(2025, 3, 11));
        assertThat(p.get(1).names()).containsExactly("reform", "eesti 200");
        // SDE's faction name matches the first period only.
        String sde = "sotsiaaldemokraatliku erakonna fraktsioon";
        assertThat(p.get(0).names().stream().anyMatch(sde::contains)).isTrue();
        assertThat(p.get(1).names().stream().anyMatch(sde::contains)).isFalse();
    }
}
