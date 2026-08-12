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
        String sourceUrl,
        String riigikoguPageUrl,
        Long rtActId,
        LocalDate rtPublished,
        List<BillVoteDto> votes,
        List<AmendmentDto> amendments
) {
    /** An amendment proposal (muudatusettepanek). {@code title} names the proposer; fileUrl is the
     *  public amendment document (may be null). */
    public record AmendmentDto(
            String externalId,
            String title,
            String reference,
            String fileUrl,
            String fileName
    ) {}

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

    /** A roll-call linked to this bill — adoption votes and per-amendment votings alike. */
    public record BillVoteDto(
            UUID id,
            Integer votingNumber,
            String type,
            String description,
            Instant startedAt,
            int resultInFavor,
            int resultAgainst,
            int resultAbstained
    ) {}
}
