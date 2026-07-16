package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DraftListDto(
        Embedded _embedded,
        Page page
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Embedded(List<DraftListEntry> content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DraftListEntry(
            String uuid,
            String title,
            Integer mark,
            Integer membership,
            String draftTypeCode,
            String activeDraftStage,
            String activeDraftStatus,
            String proceedingStatus,
            String activeDraftStatusDate,
            LeadingCommittee leadingCommittee,
            String initiated,
            String amendmentsDeadline
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LeadingCommittee(String uuid, String name, String type, Boolean active) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Page(Integer size, Integer totalElements, Integer totalPages, Integer number) {}
}
