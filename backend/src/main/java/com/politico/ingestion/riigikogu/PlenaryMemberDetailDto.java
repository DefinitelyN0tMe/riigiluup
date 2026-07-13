package com.politico.ingestion.riigikogu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlenaryMemberDetailDto(
        String uuid,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String gender,
        String dateOfBirth,           // ISO yyyy-MM-dd from source
        String biography,             // HTML
        Integer parliamentSeniority,  // days
        Photo photo,
        List<Committee> committees,
        List<ElectoralDistrict> electoralDistrict,
        Faction currentFaction        // Riigikogu returns `faction` on some endpoints; verify at first fetch
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Photo(String uuid, String fileName, String fileExtension, Links _links) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Links(Href download, Href self) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Href(String href) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Committee(String uuid, String name, String position, Boolean active) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ElectoralDistrict(String uuid, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Faction(String uuid, String name) {}
}
