package com.politico.ingestion.riigikogu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VotingDetailDto(
        String uuid,
        Integer votingNumber,
        VotingListDto.CodeValue type,
        String description,
        String startDateTime,
        String endDateTime,
        Integer present,
        Integer absent,
        Integer inFavor,
        Integer against,
        Integer neutral,
        Integer abstained,
        Sitting sitting,
        List<Voter> voters
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sitting(String uuid, String title) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Voter(
            String uuid,
            String fullName,
            Faction faction,
            Decision decision
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Faction(String uuid, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Decision(String code, String value) {}
}
