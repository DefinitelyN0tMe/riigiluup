package com.riigiluup.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ComparisonDto(
        Side left,
        Side right,
        Period period,
        PairwiseAgreementDto agreement,
        List<DisagreementDto> recentDisagreements
) {
    public record Side(
            UUID id,
            String slug,
            String fullName,
            String factionName,
            String partyShortName,
            String photoUrl,
            Double groupAlignmentRate,
            int groupAlignmentMatches,
            int groupAlignmentEligible
    ) {}

    public record Period(LocalDate from, LocalDate to) {}

    public record PairwiseAgreementDto(
            int sameCount,
            int diffCount,
            int oneNotParticipatingCount,
            int totalOverlap,
            Double agreementRate,
            String methodologyNote
    ) {}

    public record DisagreementDto(
            UUID voteEventId,
            String voteEventDescription,
            String voteType,
            String startedAt,
            String leftChoice,
            String rightChoice,
            UUID billId,
            String billTitle,
            String billMark
    ) {}
}
