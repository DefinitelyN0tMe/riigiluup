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
        boolean active,
        String wikidataQid,
        String wikipediaUrlEn,
        String wikipediaUrlEt,
        String wikipediaUrlRu,
        Faction faction,
        Party party,
        List<GroupMembershipDto> committees,
        ParticipationStats participation,
        ParticipationStats attendanceChecks,
        VotingStats voting,
        GroupAlignment groupAlignment,
        String biographyHtml,
        String sourceUrl,
        List<ExternalAffiliationDto> externalAffiliations
) {
    public record Faction(String externalId, String name) {}
    public record Party(String shortName, String fullName, String colorHex, String officialUrl) {}
    public record GroupMembershipDto(String name, String shortName, String colorHex,
                                    String role, boolean active) {}
    public record GroupAlignment(
            Double rate,
            int matches,
            int eligible,
            List<Deviation> recentDeviations,
            String methodologyNote
    ) {}
    public record Deviation(
            UUID voteEventId,
            String voteEventDescription,
            String voteType,
            String startedAt,
            String memberChoice,
            String factionMajorityChoice
    ) {}
    public record ExternalAffiliationDto(
            String organization,
            String orgKind,
            String role,
            LocalDate validFrom,
            LocalDate validTo,
            String sourceUrl,
            String sourceLabel,
            String verifiedBy,
            LocalDate verifiedAt,
            String note
    ) {}
}
