package com.riigiluup.api;

import com.riigiluup.statistics.ParticipationStats;
import com.riigiluup.statistics.VotingStats;

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
        List<ExternalAffiliationDto> externalAffiliations,
        ElectionInfo election,
        List<CampaignInfo> elections,
        ActivityInfo activity,
        String education,
        String positions,
        List<PartyMembership> partyMemberships,
        List<FactionPeriod> factionHistory,
        List<PressItem> pressActivity,
        int pressTotal,
        List<GroupMembershipDto> friendshipGroups,
        List<GroupMembershipDto> supportGroups,
        List<GroupMembershipDto> delegations,
        List<OversightDto> oversight,
        int oversightTotal
) {
    /**
     * A written question or interpellation the MP put to a minister, with whether/when it was
     * answered. {@code kind} is WRITTEN_QUESTION | INTERPELLATION.
     */
    public record OversightDto(
            String kind,
            String title,
            String addresseeName,
            LocalDate submittedOn,
            LocalDate answerDeadline,
            boolean answered,
            String respondentName,
            LocalDate respondedOn
    ) {}
    /**
     * One press-activity entry ("Ajakirjandustegevus") from the Riigikogu API. {@code url} points to
     * the external article (may be null); {@code description} is the title + publication(s) line.
     */
    public record PressItem(String description, String url, LocalDate date) {}
    /** How the MP won their seat (from opendata.valimised.ee); null if not matched. */
    public record ElectionInfo(
            String electionCode,
            int personalVotes,
            String mandateType,
            Integer districtNumber,
            String partyName,
            Integer ballotNumber,
            String sourceUrl
    ) {}
    /**
     * One campaign in the MP's electoral footprint (RK / EP / KOV), elected or not. Matched by
     * name from opendata.valimised.ee (CC BY 4.0); only high-confidence matches are present.
     */
    public record CampaignInfo(
            String electionCode,
            String electionType,   // RK | EP | KOV
            int year,
            boolean elected,
            int personalVotes,
            String mandateType,    // null when not elected
            Integer districtNumber,
            String partyName,
            Integer ballotNumber,
            String sourceUrl,
            boolean historical,    // true for the pre-2023 RK history (Mölder dataset)
            String districtName    // named district, set for historical rows
    ) {}
    /** A party-membership period from Wikidata P102. Distinct from faction and electoral list. */
    public record PartyMembership(String partyLabel, String partyQid,
                                  LocalDate startDate, LocalDate endDate) {}
    /** A faction (parliamentary group) period from the Riigikogu API — the in-parliament group. */
    public record FactionPeriod(String factionName, String factionExternalId,
                                LocalDate startDate, LocalDate endDate) {}
    /** Parliamentary activity over the term (from Riigikogu API); null if not yet computed. */
    public record ActivityInfo(
            int speeches,
            int questions,
            int interpellations,
            int writtenQuestions,
            String sourceUrl
    ) {}
    public record Faction(String externalId, String name) {}
    public record Party(String shortName, String fullName, String colorHex, String officialUrl) {}
    public record GroupMembershipDto(String externalId, String name, String shortName,
                                    String colorHex, String role, boolean active) {}
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
            String factionMajorityChoice,
            UUID billId,        // linked bill (legislative item), null for procedural votes
            String billTitle,   // the bill's name, so the row shows what was voted on
            String billMark     // e.g. "644 SE"
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
