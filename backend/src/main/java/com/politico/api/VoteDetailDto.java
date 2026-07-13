package com.politico.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VoteDetailDto(
        UUID id,
        String externalId,
        Integer votingNumber,
        String type,
        String typeSourceCode,
        String description,
        String sittingExternalId,
        String sittingTitle,
        Instant startedAt,
        Instant endedAt,
        int resultInFavor,
        int resultAgainst,
        int resultAbstained,
        int resultNeutral,
        int resultPresent,
        int resultAbsent,
        List<FactionBreakdown> factionBreakdowns,
        List<IndividualVoteDto> individualVotes,
        String sourceUrl
) {
    public record FactionBreakdown(
            String factionExternalId,
            String factionName,
            int inFavor,
            int against,
            int abstained,
            int didNotVote,
            int absent,
            int present,
            int unknown,
            int total
    ) {}

    public record IndividualVoteDto(
            String memberExternalId,
            String memberSlug,
            String memberFullName,
            String factionExternalId,
            String factionName,
            String choice,
            String choiceSourceCode
    ) {}
}
