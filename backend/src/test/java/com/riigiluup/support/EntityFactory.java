package com.riigiluup.support;

import com.riigiluup.legislation.LegislationPhase;
import com.riigiluup.legislation.LegislativeItem;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.vote.IndividualVote;
import com.riigiluup.vote.VoteChoice;
import com.riigiluup.vote.VoteEvent;
import com.riigiluup.vote.VoteEventType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Small helper so repo/service integration tests don't repeat the same builder soup. */
public final class EntityFactory {

    private EntityFactory() {}

    public static PlenaryMember member(String externalId, String first, String last,
                                       String faction, boolean active) {
        Instant now = Instant.now();
        String full = first + " " + last;
        return PlenaryMember.builder()
                .externalId(externalId)
                .sourceName("riigikogu")
                .firstName(first)
                .lastName(last)
                .fullName(full)
                .slug(full.toLowerCase().replace(' ', '-') + "-" + externalId)
                .active(active)
                .factionExternalId(faction == null ? null : "F-" + faction)
                .factionName(faction)
                .importedAt(now)
                .updatedAt(now)
                .build();
    }

    public static VoteEvent voteEvent(String externalId, Instant startedAt, VoteEventType type) {
        Instant now = Instant.now();
        return VoteEvent.builder()
                .externalId(externalId)
                .sourceName("riigikogu")
                .type(type)
                .typeSourceCode("AVALIK")
                .description("vote " + externalId)
                .startedAt(startedAt)
                .endedAt(startedAt.plusSeconds(60))
                .resultInFavor(0).resultAgainst(0).resultAbstained(0)
                .resultNeutral(0).resultPresent(0).resultAbsent(0)
                .importedAt(now)
                .updatedAt(now)
                .build();
    }

    public static IndividualVote individualVote(VoteEvent ev, PlenaryMember m,
                                                VoteChoice choice) {
        return IndividualVote.builder()
                .voteEvent(ev)
                .plenaryMember(m)
                .factionExternalId(m.getFactionExternalId())
                .factionName(m.getFactionName())
                .choice(choice)
                .choiceSourceCode(choice.name())
                .importedAt(Instant.now())
                .build();
    }

    public static LegislativeItem legislativeItem(String externalId, String title,
                                                  LegislationPhase phase,
                                                  LocalDate initiatedDate) {
        Instant now = Instant.now();
        return LegislativeItem.builder()
                .externalId(externalId)
                .sourceName("riigikogu")
                .title(title)
                .phase(phase)
                .initiatedDate(initiatedDate)
                .importedAt(now)
                .updatedAt(now)
                .build();
    }
}
