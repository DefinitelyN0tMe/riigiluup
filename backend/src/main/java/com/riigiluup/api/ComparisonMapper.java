package com.riigiluup.api;

import com.riigiluup.alignment.GroupAlignmentService;
import com.riigiluup.alignment.PairwiseAgreementService;
import com.riigiluup.common.PhotoUrlRewriter;
import com.riigiluup.party.FactionPartyLinkRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.vote.IndividualVote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ComparisonMapper {

    private final FactionPartyLinkRepository factionLinks;
    private final PhotoUrlRewriter photoUrlRewriter;
    private final GroupAlignmentService groupAlignmentService;

    public ComparisonDto build(
            PlenaryMember left, PlenaryMember right,
            LocalDate fromDate, LocalDate toDate,
            PairwiseAgreementService.Result agreement,
            List<IndividualVote[]> recentDisagreements
    ) {
        Instant fromTs = fromDate.atStartOfDay(java.time.ZoneId.of("Europe/Tallinn")).toInstant();
        Instant toTs = toDate.plusDays(1).atStartOfDay(java.time.ZoneId.of("Europe/Tallinn")).toInstant();

        return new ComparisonDto(
                toSide(left, fromTs, toTs),
                toSide(right, fromTs, toTs),
                new ComparisonDto.Period(fromDate, toDate),
                new ComparisonDto.PairwiseAgreementDto(
                        agreement.sameCount(),
                        agreement.diffCount(),
                        agreement.oneNotParticipatingCount(),
                        agreement.totalOverlap(),
                        agreement.agreementRate(),
                        "Agreement rate = same choice / (same + different), among votes where BOTH MPs cast a comparable choice (FOR / AGAINST / ABSTAINED). Non-participation reported separately."
                ),
                recentDisagreements.stream()
                        .map(pair -> {
                            var ve = pair[0].getVoteEvent();
                            var bill = ve.getLegislativeItem();
                            return new ComparisonDto.DisagreementDto(
                                ve.getId(),
                                ve.getDescription(),
                                ve.getType() == null ? null : ve.getType().name(),
                                ve.getStartedAt() == null ? null : ve.getStartedAt().toString(),
                                pair[0].getChoice() == null ? null : pair[0].getChoice().name(),
                                pair[1].getChoice() == null ? null : pair[1].getChoice().name(),
                                bill == null ? null : bill.getId(),
                                bill == null ? null : bill.getTitle(),
                                PoliticianProfileMapper.billMark(bill));
                        })
                        .toList()
        );
    }

    private ComparisonDto.Side toSide(PlenaryMember m, Instant fromTs, Instant toTs) {
        String partyShort = factionLinks
                .findFirstByFactionExternalIdOrderByValidFromDesc(m.getFactionExternalId())
                .map(link -> link.getParty().getShortName())
                .orElse(null);
        GroupAlignmentService.Result ga = groupAlignmentService.forMember(m, fromTs, toTs);
        return new ComparisonDto.Side(
                m.getId(),
                m.getSlug(),
                m.getFullName(),
                m.getFactionName(),
                partyShort,
                photoUrlRewriter.toProxyPath(m.getPhotoUrl()),
                ga.rate(), ga.matches(), ga.eligible()
        );
    }
}
