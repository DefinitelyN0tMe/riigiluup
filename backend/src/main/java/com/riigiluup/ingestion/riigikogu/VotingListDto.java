package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VotingListDto(
        String uuid,
        String title,
        String sittingDateTime,
        List<VotingSummary> votings
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VotingSummary(
            String uuid,
            Integer votingNumber,
            CodeValue type,
            String description,
            String startDateTime,
            String endDateTime,
            Integer present,
            Integer absent,
            Integer inFavor,
            Integer against,
            Integer neutral,
            Integer abstained
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CodeValue(String code, String value) {}
}
