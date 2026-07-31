package com.riigiluup.ingestion.riigikogu;

import java.time.LocalDate;

/**
 * Maps a sitting date to the Riigikogu composition (koosseis) number, as the API uses it in
 * {@code membership}. Used only to backfill membership on speeches whose verbatim record did
 * not carry it; ongoing ingestion reads membership straight from the feed. Boundaries are the
 * first sitting dates of each composition.
 */
public final class RiigikoguComposition {

    private RiigikoguComposition() {}

    private static final LocalDate C15 = LocalDate.of(2023, 4, 17);
    private static final LocalDate C14 = LocalDate.of(2019, 4, 4);
    private static final LocalDate C13 = LocalDate.of(2015, 3, 30);
    private static final LocalDate C12 = LocalDate.of(2011, 3, 27);

    /** Composition number for a date, or {@code null} if the date is unknown. */
    public static Integer at(LocalDate date) {
        if (date == null) return null;
        if (!date.isBefore(C15)) return 15;
        if (!date.isBefore(C14)) return 14;
        if (!date.isBefore(C13)) return 13;
        if (!date.isBefore(C12)) return 12;
        return 11;
    }
}
