package com.riigiluup.api;

import java.util.List;

/** Pairwise comparison of two factions (parties) by how their voting majorities align. */
public record FactionComparisonDto(
        Side left,
        Side right,
        int sameCount,
        int diffCount,
        int totalOverlap,
        Double agreementRate,
        List<Disagreement> recentDisagreements,
        String methodologyNote
) {
    public record Side(String externalId, String name, long seats) {}

    public record Disagreement(
            String voteEventId,
            String description,
            String startedAt,
            String leftChoice,
            String rightChoice,
            String billId,
            String billTitle,
            String billMark
    ) {}
}
