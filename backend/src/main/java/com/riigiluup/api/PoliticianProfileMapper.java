package com.riigiluup.api;

import com.riigiluup.alignment.GroupAlignmentService;
import com.riigiluup.alignment.VoteFactionAlignment;
import com.riigiluup.alignment.VoteFactionAlignmentRepository;
import com.riigiluup.common.PhotoUrlRewriter;
import com.riigiluup.election.ElectionResultRepository;
import com.riigiluup.group.GroupMembership;
import com.riigiluup.group.GroupType;
import com.riigiluup.party.ExternalAffiliation;
import com.riigiluup.party.ExternalAffiliationRepository;
import com.riigiluup.party.FactionPartyLinkRepository;
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
import java.util.List;

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
                m.getOfficialProfileUrl());

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
                m.getOfficialProfileUrl());

        GroupAlignmentService.Result gaResult = groupAlignmentService.forMember(
                m,
                StatisticsService.TERM_START.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                java.time.Instant.now());
        List<PoliticianProfileDto.Deviation> deviations = groupAlignmentService
                .recentDeviations(m, 10).stream()
                .map(this::toDeviationDto)
                .toList();
        PoliticianProfileDto.GroupAlignment ga = new PoliticianProfileDto.GroupAlignment(
                gaResult.rate(), gaResult.matches(), gaResult.eligible(), deviations,
                "Group alignment = (MP matched faction majority) / (eligible votes where faction had a clear majority)."
        );

        List<PoliticianProfileDto.ExternalAffiliationDto> external = externalAffiliations
                .findByMemberSlugOrderByValidFromAsc(m.getSlug()).stream()
                .map(this::toExternalDto)
                .toList();

        PoliticianProfileDto.ElectionInfo election = electionResults
                .findFirstByMemberExternalIdOrderByElectionCodeDesc(m.getExternalId())
                .map(er -> new PoliticianProfileDto.ElectionInfo(
                        er.getElectionCode(), er.getPersonalVotes(), er.getMandateType(),
                        er.getDistrictNumber(), er.getPartyName(), er.getBallotNumber(),
                        "https://rk2023.valimised.ee/et/election-result"))
                .orElse(null);

        return new PoliticianProfileDto(
                m.getId(), m.getSlug(),
                m.getFullName(), m.getFirstName(), m.getLastName(),
                photoUrlRewriter.toProxyPath(m.getPhotoUrl()), m.getOfficialProfileUrl(),
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
                election
        );
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

    private PoliticianProfileDto.Deviation toDeviationDto(IndividualVote iv) {
        VoteEvent ev = iv.getVoteEvent();
        String majority = alignmentRepo
                .findByVoteEventAndFactionExternalId(ev, iv.getFactionExternalId())
                .map(VoteFactionAlignment::getMajorityChoice)
                .map(Enum::name)
                .orElse(null);
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
