package com.riigiluup.analytics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsParamsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 22);

    @Test
    void clamp_bounds_ints() {
        assertThat(AnalyticsParams.clamp(5, 1, 10)).isEqualTo(5);
        assertThat(AnalyticsParams.clamp(-3, 1, 10)).isEqualTo(1);
        assertThat(AnalyticsParams.clamp(999, 1, 10)).isEqualTo(10);
    }

    @Test
    void neighbouring_from_dates_in_same_month_collapse_to_one_key() {
        // Two nearby days in March 2025 → same normalized lower bound → same cache key.
        LocalDate a = AnalyticsParams.normalizeFrom(LocalDate.of(2025, 3, 4), TODAY);
        LocalDate b = AnalyticsParams.normalizeFrom(LocalDate.of(2025, 3, 27), TODAY);
        assertThat(a).isEqualTo(b).isEqualTo(LocalDate.of(2025, 3, 1));
    }

    @Test
    void neighbouring_to_dates_in_same_month_collapse_to_one_key() {
        LocalDate a = AnalyticsParams.normalizeTo(LocalDate.of(2025, 3, 4), TODAY);
        LocalDate b = AnalyticsParams.normalizeTo(LocalDate.of(2025, 3, 27), TODAY);
        assertThat(a).isEqualTo(b).isEqualTo(LocalDate.of(2025, 3, 31));
    }

    @Test
    void dates_are_clamped_into_the_valid_range() {
        // Before MIN → MIN (rounded to month start).
        assertThat(AnalyticsParams.normalizeFrom(LocalDate.of(1999, 5, 9), TODAY))
                .isEqualTo(LocalDate.of(2023, 1, 1));
        // After today → today's month.
        assertThat(AnalyticsParams.normalizeTo(LocalDate.of(3000, 1, 1), TODAY))
                .isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void null_dates_stay_null() {
        assertThat(AnalyticsParams.normalizeFrom(null, TODAY)).isNull();
        assertThat(AnalyticsParams.normalizeTo(null, TODAY)).isNull();
    }
}
