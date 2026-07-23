package com.riigiluup.api;

import java.time.Instant;
import java.util.UUID;

public record VoteListItemDto(
        UUID id,
        String externalId,
        Integer votingNumber,
        String type,
        String typeSourceCode,
        String description,
        String sittingTitle,
        String billTitle,
        Instant startedAt,
        int resultInFavor,
        int resultAgainst,
        int resultAbstained,
        int resultNeutral,
        int resultPresent,
        int resultAbsent,
        String sourceUrl
) {}
