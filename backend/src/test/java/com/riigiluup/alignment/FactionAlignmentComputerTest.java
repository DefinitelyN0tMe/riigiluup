package com.riigiluup.alignment;

import com.riigiluup.person.PlenaryMember;
import com.riigiluup.vote.IndividualVote;
import com.riigiluup.vote.VoteChoice;
import com.riigiluup.vote.VoteEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FactionAlignmentComputerTest {

    private final FactionAlignmentComputer computer = new FactionAlignmentComputer();

    private static IndividualVote iv(VoteChoice c) {
        return IndividualVote.builder()
                .id(UUID.randomUUID())
                .voteEvent(VoteEvent.builder().id(UUID.randomUUID()).build())
                .plenaryMember(PlenaryMember.builder().id(UUID.randomUUID()).build())
                .choice(c)
                .importedAt(Instant.now())
                .build();
    }

    @Test
    void picks_modal_comparable_choice_as_majority() {
        List<IndividualVote> votes = List.of(
                iv(VoteChoice.FOR), iv(VoteChoice.FOR), iv(VoteChoice.FOR),
                iv(VoteChoice.AGAINST),
                iv(VoteChoice.ABSTAINED),
                iv(VoteChoice.DID_NOT_VOTE),
                iv(VoteChoice.ABSENT)
        );

        FactionAlignmentComputer.Result r = computer.compute(votes);

        assertThat(r.comparableCount()).isEqualTo(5);
        assertThat(r.majorityChoice()).isEqualTo(VoteChoice.FOR);
        assertThat(r.majorityCount()).isEqualTo(3);
        assertThat(r.hasClearMajority()).isTrue();
    }

    @Test
    void ties_do_not_count_as_clear_majority() {
        List<IndividualVote> votes = List.of(
                iv(VoteChoice.FOR), iv(VoteChoice.FOR),
                iv(VoteChoice.AGAINST), iv(VoteChoice.AGAINST)
        );

        FactionAlignmentComputer.Result r = computer.compute(votes);

        assertThat(r.comparableCount()).isEqualTo(4);
        assertThat(r.hasClearMajority()).isFalse();
        assertThat(r.majorityChoice()).isNull();
        assertThat(r.majorityCount()).isEqualTo(2);
    }

    @Test
    void empty_or_all_non_participating_has_no_majority() {
        FactionAlignmentComputer.Result empty = computer.compute(List.of());
        assertThat(empty.hasClearMajority()).isFalse();
        assertThat(empty.comparableCount()).isZero();

        FactionAlignmentComputer.Result nonPart = computer.compute(List.of(
                iv(VoteChoice.DID_NOT_VOTE), iv(VoteChoice.ABSENT), iv(VoteChoice.PRESENT)));
        assertThat(nonPart.hasClearMajority()).isFalse();
        assertThat(nonPart.comparableCount()).isZero();
        assertThat(nonPart.majorityChoice()).isNull();
    }

    @Test
    void single_comparable_vote_is_a_clear_majority_of_one() {
        FactionAlignmentComputer.Result r = computer.compute(List.of(
                iv(VoteChoice.AGAINST), iv(VoteChoice.DID_NOT_VOTE)));

        assertThat(r.comparableCount()).isEqualTo(1);
        assertThat(r.majorityCount()).isEqualTo(1);
        assertThat(r.hasClearMajority()).isTrue();
        assertThat(r.majorityChoice()).isEqualTo(VoteChoice.AGAINST);
    }
}
