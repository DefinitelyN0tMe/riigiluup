package com.riigiluup.api;

import com.riigiluup.alignment.GroupAlignmentService;
import com.riigiluup.alignment.VoteFactionAlignment;
import com.riigiluup.alignment.VoteFactionAlignmentRepository;
import com.riigiluup.activity.MemberActivityRepository;
import com.riigiluup.common.PhotoUrlRewriter;
import com.riigiluup.election.ElectionResult;
import com.riigiluup.election.ElectionResultRepository;
import com.riigiluup.group.GroupMembership;
import com.riigiluup.group.GroupType;
import com.riigiluup.party.ExternalAffiliation;
import com.riigiluup.party.ExternalAffiliationRepository;
import com.riigiluup.party.FactionPartyLinkRepository;
import com.riigiluup.person.MpPartyMembershipRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.statistics.ParticipationStats;
import com.riigiluup.statistics.StatisticsService;
import com.riigiluup.statistics.VotingStats;
import com.riigiluup.vote.IndividualVote;
import com.riigiluup.vote.IndividualVoteRepository;
import com.riigiluup.vote.VoteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PoliticianProfileMapper {

    private final FactionPartyLinkRepository factionLinks;
    private final StatisticsService stats;
    private final PhotoUrlRewriter photoUrlRewriter;
    private final GroupAlignmentService groupAlignmentService;
    private final VoteFactionAlignmentRepository alignmentRepo;
    private final ExternalAffiliationRepository externalAffiliations;
    private final IndividualVoteRepository individualVoteRepo;
    private final ElectionResultRepository electionResults;
    private final MpPartyMembershipRepository partyMembershipRepo;
    private final com.riigiluup.person.MpFactionMembershipRepository factionHistoryRepo;
    private final MemberActivityRepository memberActivity;

    public PoliticianProfileDto toDto(PlenaryMember m, List<GroupMembership> memberships) {
        PoliticianProfileDto.Party party = factionLinks
                .findFirstByFactionExternalIdOrderByValidFromDesc(m.getFactionExternalId())
                .map(link -> new PoliticianProfileDto.Party(
                        link.getParty().getShortName(),
                        link.getParty().getFullName(),
                        link.getParty().getColorHex(),
                        link.getParty().getOfficialUrl()))
                .orElse(null);

        List<PoliticianProfileDto.GroupMembershipDto> committees = memberships.stream()
                .filter(gm -> gm.getGroup() != null)
                .filter(gm -> gm.getGroup().getType() == GroupType.STANDING_COMMITTEE
                           || gm.getGroup().getType() == GroupType.SPECIAL_COMMITTEE)
                .map(gm -> new PoliticianProfileDto.GroupMembershipDto(
                        gm.getGroup().getExternalId(),
                        gm.getGroup().getName(),
                        gm.getGroup().getShortName(),
                        gm.getGroup().getColorHex(),
                        gm.getRole().name(),
                        gm.isActive()))
                .toList();

        LocalDate today = LocalDate.now();
        ParticipationStats participation = stats.participation(
                m.getExternalId(), StatisticsService.TERM_START, today);

        // Voting participation is computed from our OWN ingested roll-call votes rather than the
        // fragile live Riigikogu statistics passthrough — which returned 0/0 whenever the source
        // API was rate-limited (e.g. during a backfill), making an active MP look like a non-voter.
        // Estonia has 101 MPs and a stable record, so the local aggregate is authoritative and cheap.
        java.time.Instant termStartTs = StatisticsService.TERM_START
                .atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        IndividualVoteRepository.ParticipationAgg vAgg =
                individualVoteRepo.aggregateVotingParticipation(m.getId(), termStartTs);
        int vTotal = (int) vAgg.getTotal();
        int vParticipated = (int) vAgg.getParticipated();
        Double vRate = vTotal == 0 ? null : (double) vParticipated / vTotal;
        VotingStats voting = new VotingStats(
                vTotal, vParticipated, vRate,
                "Voting participation = (FOR + AGAINST + ABSTAINED) / eligible roll-call votes, "
                        + "computed from ingested Riigikogu roll-call records (current term, since 2023-04-10).",
                riigikoguMemberUrl(m));

        // Our own quorum-check (kohalolekukontroll) presence — a stricter, per-moment measure than
        // the Riigikogu sitting-attendance statistic above; shown alongside it so both are visible.
        IndividualVoteRepository.AttendanceAgg cAgg =
                individualVoteRepo.aggregateAttendanceChecks(m.getId(), termStartTs);
        int cTotal = (int) cAgg.getTotal();
        int cPresent = (int) cAgg.getPresent();
        Double cRate = cTotal == 0 ? null : (double) cPresent / cTotal;
        ParticipationStats attendanceChecks = new ParticipationStats(
                cTotal, cPresent, cRate,
                "Quorum-check presence = KOHAL / (KOHAL + PUUDUB), computed from ingested "
                        + "kohalolekukontroll records (current term, since 2023-04-10).",
                riigikoguMemberUrl(m));

        GroupAlignmentService.Result gaResult = groupAlignmentService.forMember(
                m,
                StatisticsService.TERM_START.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                java.time.Instant.now());
        List<IndividualVote> deviationVotes = groupAlignmentService.recentDeviations(m, 10);
        Map<String, VoteFactionAlignment> deviationAlignments = alignmentsFor(deviationVotes);
        List<PoliticianProfileDto.Deviation> deviations = deviationVotes.stream()
                .map(iv -> toDeviationDto(iv, deviationAlignments))
                .toList();
        PoliticianProfileDto.GroupAlignment ga = new PoliticianProfileDto.GroupAlignment(
                gaResult.rate(), gaResult.matches(), gaResult.eligible(), deviations,
                "Group alignment = (MP matched faction majority) / (eligible votes where faction had a clear majority)."
        );

        List<PoliticianProfileDto.ExternalAffiliationDto> external = externalAffiliations
                .findByMemberSlugOrderByValidFromAsc(m.getSlug()).stream()
                .map(this::toExternalDto)
                .toList();

        List<ElectionResult> allCampaigns = electionResults.findByMemberExternalId(m.getExternalId());

        // The seat block stays exactly as before: the Riigikogu (RK) result, i.e. how the MP
        // won their seat. Other elections (EP/KOV) are surfaced separately as a footprint list.
        PoliticianProfileDto.ElectionInfo election = allCampaigns.stream()
                .filter(er -> !er.isHistorical()
                        && er.getElectionCode() != null && er.getElectionCode().startsWith("RK_"))
                .findFirst()
                .map(er -> new PoliticianProfileDto.ElectionInfo(
                        er.getElectionCode(), er.getPersonalVotes(), er.getMandateType(),
                        er.getDistrictNumber(), er.getPartyName(), er.getBallotNumber(),
                        "https://rk2023.valimised.ee/et/election-result"))
                .orElse(null);

        // The full electoral footprint (RK / EP / KOV), newest campaign first.
        List<PoliticianProfileDto.CampaignInfo> elections = allCampaigns.stream()
                .map(PoliticianProfileMapper::toCampaign)
                .sorted(Comparator.comparingInt(PoliticianProfileDto.CampaignInfo::year).reversed()
                        .thenComparing(PoliticianProfileDto.CampaignInfo::electionType))
                .toList();

        PoliticianProfileDto.ActivityInfo activity = memberActivity
                .findById(m.getExternalId())
                .map(a -> new PoliticianProfileDto.ActivityInfo(
                        a.getSpeeches(), a.getQuestions(), a.getInterpellations(), a.getWrittenQuestions(),
                        riigikoguMemberUrl(m)))
                .orElse(null);

        List<PoliticianProfileDto.PartyMembership> partyMemberships = partyMembershipRepo
                .findByMemberExternalIdOrderByStartDateAsc(m.getExternalId()).stream()
                .map(pm -> new PoliticianProfileDto.PartyMembership(
                        pm.getPartyLabel(), pm.getPartyQid(), pm.getStartDate(), pm.getEndDate()))
                .toList();

        // Drop the short "unaffiliated" gaps at term formation (a couple of days before factions
        // register) so the timeline shows real moves, not noise; a genuine departure is open-ended.
        List<PoliticianProfileDto.FactionPeriod> factionHistory = factionHistoryRepo
                .findByMemberExternalIdOrderByStartDateAsc(m.getExternalId()).stream()
                .filter(f -> !isTransientUnaffiliated(f))
                .map(f -> new PoliticianProfileDto.FactionPeriod(
                        f.getFactionName(), f.getFactionExternalId(), f.getStartDate(), f.getEndDate()))
                .toList();

        return new PoliticianProfileDto(
                m.getId(), m.getSlug(),
                m.getFullName(), m.getFirstName(), m.getLastName(),
                photoUrlRewriter.toProxyPath(m.getPhotoUrl()), riigikoguMemberUrl(m),
                m.getEmail(), m.getGender(), m.getDateOfBirth(),
                m.getElectoralDistrict(),
                m.getParliamentSeniorityDays(),
                m.isActive(),
                m.getWikidataQid(),
                m.getWikipediaUrlEn(),
                m.getWikipediaUrlEt(),
                m.getWikipediaUrlRu(),
                m.getFactionExternalId() == null ? null
                        : new PoliticianProfileDto.Faction(
                                m.getFactionExternalId(), m.getFactionName()),
                party,
                committees,
                participation, attendanceChecks, voting,
                ga,
                m.getBiographyHtml(),
                "https://api.riigikogu.ee/api/plenary-members/" + m.getExternalId(),
                external,
                election,
                elections,
                activity,
                m.getEducation(),
                m.getPositions(),
                partyMemberships,
                factionHistory
        );
    }

    /**
     * Working human-readable Riigikogu profile page. The URL the API stores
     * (…/riigikogu-liikmed/liige/{uuid}/) now 404s after a site restructure; the current
     * page lives under …/saadik/{uuid}/{name}/ and resolves by UUID (the name is decorative).
     */
    private static String riigikoguMemberUrl(PlenaryMember m) {
        String slug = m.getFullName() == null ? "" : m.getFullName().trim().replace(' ', '-');
        return "https://www.riigikogu.ee/riigikogu/koosseis/riigikogu-liikmed/saadik/"
                + m.getExternalId() + "/" + slug + "/";
    }

    private PoliticianProfileDto.ExternalAffiliationDto toExternalDto(ExternalAffiliation e) {
        return new PoliticianProfileDto.ExternalAffiliationDto(
                e.getOrganization(), e.getOrgKind(), e.getRole(),
                e.getValidFrom(), e.getValidTo(),
                e.getSourceUrl(), e.getSourceLabel(),
                e.getVerifiedBy(), e.getVerifiedAt(),
                e.getNote()
        );
    }

    /** One batched alignment fetch for the deviation list instead of a query per deviation. */
    private Map<String, VoteFactionAlignment> alignmentsFor(List<IndividualVote> votes) {
        List<VoteEvent> events = votes.stream()
                .map(IndividualVote::getVoteEvent)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (events.isEmpty()) return Map.of();
        return alignmentRepo.findByVoteEventIn(events).stream()
                .collect(Collectors.toMap(
                        a -> a.getVoteEvent().getId() + "|" + a.getFactionExternalId(),
                        Function.identity(), (a, b) -> a));
    }

    /** Maps a stored campaign row to the DTO, deriving type/year and the results-site link. */
    private static PoliticianProfileDto.CampaignInfo toCampaign(ElectionResult er) {
        String code = er.getElectionCode();
        String[] parts = code.split("_", 2);
        String type = parts[0];
        int year = parts.length > 1 ? parseYear(parts[1]) : 0;
        // Historical (Mölder) rows point to eestipoliitika.ee (his data site); open-data rows to
        // the official results site, e.g. RK_2023 -> https://rk2023.valimised.ee.
        String sourceUrl = er.isHistorical()
                ? "https://www.eestipoliitika.ee"
                : "https://" + type.toLowerCase(java.util.Locale.ROOT) + year + ".valimised.ee";
        return new PoliticianProfileDto.CampaignInfo(
                code, type, year, er.isElected(), er.getPersonalVotes(), er.getMandateType(),
                er.getDistrictNumber(), er.getPartyName(), er.getBallotNumber(), sourceUrl,
                er.isHistorical(), er.getDistrictName());
    }

    /** A brief (&lt; 30 day) closed "unaffiliated" span — the term-formation gap, not a real move. */
    private static boolean isTransientUnaffiliated(com.riigiluup.person.MpFactionMembership f) {
        String name = f.getFactionName();
        if (name == null || !name.toLowerCase(java.util.Locale.ROOT).contains("mittekuuluv")) return false;
        if (f.getStartDate() == null || f.getEndDate() == null) return false; // current/open span -> keep
        return java.time.temporal.ChronoUnit.DAYS.between(f.getStartDate(), f.getEndDate()) < 30;
    }

    private static int parseYear(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private PoliticianProfileDto.Deviation toDeviationDto(
            IndividualVote iv, Map<String, VoteFactionAlignment> alignments) {
        VoteEvent ev = iv.getVoteEvent();
        VoteFactionAlignment alignment = ev == null ? null
                : alignments.get(ev.getId() + "|" + iv.getFactionExternalId());
        String majority = alignment == null || alignment.getMajorityChoice() == null
                ? null : alignment.getMajorityChoice().name();
        return new PoliticianProfileDto.Deviation(
                ev == null ? null : ev.getId(),
                ev == null ? null : ev.getDescription(),
                ev == null || ev.getType() == null ? null : ev.getType().name(),
                ev == null || ev.getStartedAt() == null ? null : ev.getStartedAt().toString(),
                iv.getChoice() == null ? null : iv.getChoice().name(),
                majority
        );
    }
}
