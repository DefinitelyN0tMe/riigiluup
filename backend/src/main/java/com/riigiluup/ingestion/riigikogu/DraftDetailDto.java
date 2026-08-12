package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DraftDetailDto(
        String uuid,
        String title,
        String initialTitle,
        Integer mark,
        Integer membership,
        String draftTypeCode,
        String activeDraftStage,
        String activeDraftStatus,
        String introduction,
        String initiated,
        String accepted,
        String amendmentsDeadline,
        DraftListDto.LeadingCommittee leadingCommittee,
        List<Initiator> initiators,
        List<Descriptor> descriptors,
        List<Reading> readings,
        List<Amendment> amendments
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Initiator(String uuid, String name, String type, Boolean active) {}

    /** An amendment proposal (muudatusettepanek); {@code title} names the proposer + bill. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amendment(String uuid, String reference, String title, String documentType,
                            List<FileRef> files) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FileRef(String uuid, String fileName, String fileExtension,
                          String accessRestrictionType) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Descriptor(Integer edid, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reading(String readingCode, List<ProceedingEvent> proceedingEvents) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProceedingEvent(String date, String status) {}
}
