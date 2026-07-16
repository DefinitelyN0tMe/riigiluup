package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.person.PlenaryMember;
import com.riigiluup.vote.IndividualVote;
import com.riigiluup.vote.VoteChoice;
import com.riigiluup.vote.VoteEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class IndividualVoteMapper {

    public IndividualVote toEntity(
            VoteEvent event, PlenaryMember member, VotingDetailDto.Voter voter
    ) {
        String factionUuid = voter.faction() == null ? null : voter.faction().uuid();
        String factionName = voter.faction() == null ? null : voter.faction().name();
        String code = voter.decision() == null ? null : voter.decision().code();
        return IndividualVote.builder()
                .voteEvent(event)
                .plenaryMember(member)
                .factionExternalId(factionUuid)
                .factionName(factionName)
                .choice(VoteChoice.fromSourceCode(code))
                .choiceSourceCode(code)
                .importedAt(Instant.now())
                .build();
    }
}
