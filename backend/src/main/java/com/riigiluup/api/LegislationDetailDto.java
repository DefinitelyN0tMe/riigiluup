package com.riigiluup.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LegislationDetailDto(
        UUID id,
        String externalId,
        Integer mark,
        Integer membership,
        String draftTypeCode,
        String title,
        String initialTitle,
        String phase,
        String activeStageSourceCode,
        String activeStatusSourceCode,
        String proceedingStatus,
        LocalDate activeStatusDate,
        LocalDate initiatedDate,
        LocalDate acceptedDate,
        Instant amendmentsDeadline,
        String introduction,
        String leadingCommitteeName,
        List<StageDto> stages,
        List<SponsorDto> sponsors,
        List<TopicDto> topics,
        String sourceUrl
) {
    public record StageDto(
            String readingCode,
            String statusCode,
            Instant occurredAt,
            int sequence
    ) {}

    public record SponsorDto(
            String kind,
            String displayName,
            String memberSlug,
            String memberFullName,
            String externalId
    ) {}

    public record TopicDto(int edid, String text) {}
}
