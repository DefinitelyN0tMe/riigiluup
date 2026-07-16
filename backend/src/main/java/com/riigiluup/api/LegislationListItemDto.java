package com.riigiluup.api;

import java.time.LocalDate;
import java.util.UUID;

public record LegislationListItemDto(
        UUID id,
        String externalId,
        Integer mark,
        String draftTypeCode,
        String title,
        String phase,
        String activeStageSourceCode,
        LocalDate initiatedDate,
        LocalDate acceptedDate,
        String leadingCommitteeName,
        String sourceUrl
) {}
