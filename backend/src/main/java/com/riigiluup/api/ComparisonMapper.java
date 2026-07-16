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
                        .map(pair -> new ComparisonDto.DisagreementDto(
                                pair[0].getVoteEvent().getId(),
                                pair[0].getVoteEvent().getDescription(),
                                pair[0].getVoteEvent().getType() == null ? null
                                        : pair[0].getVoteEvent().getType().name(),
                                pair[0].getVoteEvent().getStartedAt() == null ? null
                                        : pair[0].getVoteEvent().getStartedAt().toString(),
                                pair[0].getChoice() == null ? null : pair[0].getChoice().name(),
                                pair[1].getChoice() == null ? null : pair[1].getChoice().name()))
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
