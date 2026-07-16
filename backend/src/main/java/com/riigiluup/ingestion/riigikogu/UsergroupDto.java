package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UsergroupDto(
        String uuid,
        String name,
        String shortName,
        Boolean active,
        String colorHex,
        String secretariatName,
        Type type
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Type(String code, String value) {}
}
