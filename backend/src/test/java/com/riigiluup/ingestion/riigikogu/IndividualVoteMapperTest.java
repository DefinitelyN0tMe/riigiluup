package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.person.PlenaryMember;
import com.riigiluup.vote.IndividualVote;
import com.riigiluup.vote.VoteChoice;
import com.riigiluup.vote.VoteEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IndividualVoteMapperTest {

    private final IndividualVoteMapper mapper = new IndividualVoteMapper();

    @Test
    void snapshots_faction_and_maps_choice_from_source_code() {
        VoteEvent event = VoteEvent.builder().externalId("v-1")
                .sourceName("riigikogu")
                .importedAt(Instant.now()).updatedAt(Instant.now())
                .build();
        PlenaryMember member = PlenaryMember.builder()
                .externalId("m-1").sourceName("riigikogu")
                .firstName("Jaak").lastName("Aab").fullName("Jaak Aab")
                .slug("jaak-aab").active(true)
                .importedAt(Instant.now()).updatedAt(Instant.now())
                .build();
        VotingDetailDto.Voter voter = new VotingDetailDto.Voter(
                "m-1", "Jaak Aab",
                new VotingDetailDto.Faction("f-x", "Fraktsiooni mittekuuluvad Riigikogu liikmed"),
                new VotingDetailDto.Decision("EI_HAALETANUD", "ei hääletanud"));

        IndividualVote iv = mapper.toEntity(event, member, voter);

        assertThat(iv.getVoteEvent()).isSameAs(event);
        assertThat(iv.getPlenaryMember()).isSameAs(member);
        assertThat(iv.getFactionExternalId()).isEqualTo("f-x");
        assertThat(iv.getFactionName()).isEqualTo("Fraktsiooni mittekuuluvad Riigikogu liikmed");
        assertThat(iv.getChoice()).isEqualTo(VoteChoice.DID_NOT_VOTE);
        assertThat(iv.getChoiceSourceCode()).isEqualTo("EI_HAALETANUD");
        assertThat(iv.getImportedAt()).isNotNull();
    }

    @Test
    void handles_missing_faction_and_null_decision() {
        VoteEvent event = VoteEvent.builder().externalId("v-2")
                .sourceName("riigikogu")
                .importedAt(Instant.now()).updatedAt(Instant.now())
                .build();
        PlenaryMember member = PlenaryMember.builder()
                .externalId("m-2").sourceName("riigikogu")
                .firstName("A").lastName("B").fullName("A B")
                .slug("a-b").active(true)
                .importedAt(Instant.now()).updatedAt(Instant.now())
                .build();
        VotingDetailDto.Voter voter = new VotingDetailDto.Voter(
                "m-2", "A B", null, null);

        IndividualVote iv = mapper.toEntity(event, member, voter);

        assertThat(iv.getFactionExternalId()).isNull();
        assertThat(iv.getFactionName()).isNull();
        assertThat(iv.getChoice()).isEqualTo(VoteChoice.UNKNOWN);
        assertThat(iv.getChoiceSourceCode()).isNull();
    }
}
