package com.politico.api;

import com.politico.vote.IndividualVote;
import com.politico.vote.VoteChoice;
import com.politico.vote.VoteEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class VoteDetailMapper {

    public VoteListItemDto toListItem(VoteEvent v) {
        return new VoteListItemDto(
                v.getId(), v.getExternalId(), v.getVotingNumber(),
                v.getType() == null ? "OTHER" : v.getType().name(),
                v.getTypeSourceCode(),
                v.getDescription(),
                v.getSittingTitle(),
                v.getStartedAt(),
                v.getResultInFavor(), v.getResultAgainst(), v.getResultAbstained(),
                v.getResultNeutral(), v.getResultPresent(), v.getResultAbsent(),
                sourceUrl(v.getExternalId())
        );
    }

    public VoteDetailDto toDetail(VoteEvent v, List<IndividualVote> votes) {
        Map<String, FactionAcc> byFaction = new LinkedHashMap<>();
        List<VoteDetailDto.IndividualVoteDto> individuals = new ArrayList<>();

        List<IndividualVote> sorted = votes.stream()
                .sorted(Comparator.comparing((IndividualVote iv) ->
                                iv.getFactionName() == null ? "~" : iv.getFactionName())
                        .thenComparing(iv -> iv.getPlenaryMember() == null ? ""
                                : iv.getPlenaryMember().getLastName()))
                .toList();

        for (IndividualVote iv : sorted) {
            String factionUuid = iv.getFactionExternalId();
            String factionName = iv.getFactionName() == null ? "Unaffiliated" : iv.getFactionName();
            FactionAcc acc = byFaction.computeIfAbsent(
                    factionUuid == null ? "" : factionUuid,
                    k -> new FactionAcc(factionUuid, factionName));
            acc.add(iv.getChoice());

            individuals.add(new VoteDetailDto.IndividualVoteDto(
                    iv.getPlenaryMember() == null ? null : iv.getPlenaryMember().getExternalId(),
                    iv.getPlenaryMember() == null ? null : iv.getPlenaryMember().getSlug(),
                    iv.getPlenaryMember() == null ? null : iv.getPlenaryMember().getFullName(),
                    iv.getFactionExternalId(),
                    iv.getFactionName(),
                    iv.getChoice() == null ? "UNKNOWN" : iv.getChoice().name(),
                    iv.getChoiceSourceCode()
            ));
        }

        List<VoteDetailDto.FactionBreakdown> breakdowns = byFaction.values().stream()
                .map(FactionAcc::toDto)
                .toList();

        return new VoteDetailDto(
                v.getId(), v.getExternalId(), v.getVotingNumber(),
                v.getType() == null ? "OTHER" : v.getType().name(),
                v.getTypeSourceCode(),
                v.getDescription(),
                v.getSittingExternalId(), v.getSittingTitle(),
                v.getStartedAt(), v.getEndedAt(),
                v.getResultInFavor(), v.getResultAgainst(), v.getResultAbstained(),
                v.getResultNeutral(), v.getResultPresent(), v.getResultAbsent(),
                breakdowns, individuals,
                sourceUrl(v.getExternalId())
        );
    }

    private static String sourceUrl(String uuid) {
        return "https://api.riigikogu.ee/api/votings/" + uuid;
    }

    private static final class FactionAcc {
        final String uuid;
        final String name;
        int inFavor, against, abstained, didNotVote, absent, present, unknown;

        FactionAcc(String uuid, String name) { this.uuid = uuid; this.name = name; }

        void add(VoteChoice c) {
            if (c == null) { unknown++; return; }
            switch (c) {
                case FOR -> inFavor++;
                case AGAINST -> against++;
                case ABSTAINED -> abstained++;
                case DID_NOT_VOTE -> didNotVote++;
                case ABSENT -> absent++;
                case PRESENT -> present++;
                case UNKNOWN -> unknown++;
            }
        }

        VoteDetailDto.FactionBreakdown toDto() {
            int total = inFavor + against + abstained + didNotVote + absent + present + unknown;
            return new VoteDetailDto.FactionBreakdown(
                    uuid, name,
                    inFavor, against, abstained, didNotVote, absent, present, unknown, total);
        }
    }
}
