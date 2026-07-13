package com.politico.api;

import com.politico.statistics.ParticipationStats;
import com.politico.statistics.VotingStats;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PoliticianProfileDto(
        UUID id,
        String slug,
        String fullName,
        String firstName,
        String lastName,
        String photoUrl,
        String officialProfileUrl,
        String email,
        String gender,
        LocalDate dateOfBirth,
        String electoralDistrict,
        Integer parliamentSeniorityDays,
        Faction faction,
        Party party,
        List<GroupMembershipDto> committees,
        ParticipationStats participation,
        VotingStats voting,
        String biographyHtml,
        String sourceUrl
) {
    public record Faction(String externalId, String name) {}
    public record Party(String shortName, String fullName, String colorHex, String officialUrl) {}
    public record GroupMembershipDto(String name, String shortName, String colorHex,
                                    String role, boolean active) {}
}
