package com.riigiluup.analytics;

import java.time.LocalDate;

/**
 * Pure parameter normalisation for the analytics endpoints. Clamping/rounding here bounds the
 * cardinality of the {@code @Cacheable} keys downstream: requests that differ only by junk or by
 * a few days collapse onto the same key instead of spawning a fresh (expensive) aggregation each.
 */
final class AnalyticsParams {

    /** Earliest sensible query date — the corpus starts with the 2023 term. */
    static final LocalDate MIN_DATE = LocalDate.of(2023, 1, 1);

    private AnalyticsParams() {}

    static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    static LocalDate clampDate(LocalDate d, LocalDate min, LocalDate max) {
        if (d.isBefore(min)) return min;
        if (d.isAfter(max)) return max;
        return d;
    }

    /** Round a lower-bound date down to the first of its month, clamped to [MIN_DATE, today]. */
    static LocalDate normalizeFrom(LocalDate d, LocalDate today) {
        if (d == null) return null;
        return clampDate(d, MIN_DATE, today).withDayOfMonth(1);
    }

    /** Round an inclusive upper-bound date up to the last day of its month, clamped to [MIN_DATE, today]. */
    static LocalDate normalizeTo(LocalDate d, LocalDate today) {
        if (d == null) return null;
        LocalDate clamped = clampDate(d, MIN_DATE, today);
        return clamped.withDayOfMonth(1).plusMonths(1).minusDays(1);
    }
}
