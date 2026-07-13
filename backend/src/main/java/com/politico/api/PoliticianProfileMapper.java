package com.politico.api;

import com.politico.common.PhotoUrlRewriter;
import com.politico.group.GroupMembership;
import com.politico.group.GroupType;
import com.politico.party.FactionPartyLinkRepository;
import com.politico.person.PlenaryMember;
import com.politico.statistics.ParticipationStats;
import com.politico.statistics.StatisticsService;
import com.politico.statistics.VotingStats;
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
        VotingStats voting = stats.voting(
                m.getExternalId(), StatisticsService.TERM_START, today);

        return new PoliticianProfileDto(
                m.getId(), m.getSlug(),
                m.getFullName(), m.getFirstName(), m.getLastName(),
                photoUrlRewriter.toProxyPath(m.getPhotoUrl()), m.getOfficialProfileUrl(),
                m.getEmail(), m.getGender(), m.getDateOfBirth(),
                m.getElectoralDistrict(),
                m.getParliamentSeniorityDays(),
                m.getFactionExternalId() == null ? null
                        : new PoliticianProfileDto.Faction(
                                m.getFactionExternalId(), m.getFactionName()),
                party,
                committees,
                participation, voting,
                m.getBiographyHtml(),
                "https://api.riigikogu.ee/api/plenary-members/" + m.getExternalId()
        );
    }
}
