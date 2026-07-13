package com.politico.ingestion.riigikogu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlenaryMemberDto(
        String uuid,
        String firstName,
        String lastName,
        String fullName,
        Boolean active,
        Faction faction,
        String photoUrl
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Faction(String uuid, String name) {}
}
