package com.riigiluup.api;

import java.time.Instant;
import java.util.List;

/**
 * Live figures for the homepage stat strip — replaces the previously hard-coded deltas,
 * sparklines and "next sync" placeholder so the numbers actually reflect the data.
 *
 * @param lastSyncAt     finish time of the most recent successful/partial import (null if none yet)
 * @param nextSyncAt     next scheduled votes-refresh fire time (from the real cron)
 * @param votesThisWeek  vote events recorded in the last 7 days
 * @param billsInProgress bills still under proceeding (not adopted/rejected/withdrawn)
 * @param votesPerDay    vote counts for the last 14 days, oldest → newest (sparkline)
 * @param billsPerWeek   bill-initiation counts for the last 12 weeks, oldest → newest (sparkline)
 */
public record HomeSummaryDto(
        Instant lastSyncAt,
        Instant nextSyncAt,
        long votesThisWeek,
        long billsInProgress,
        List<Integer> votesPerDay,
        List<Integer> billsPerWeek
) {
}
