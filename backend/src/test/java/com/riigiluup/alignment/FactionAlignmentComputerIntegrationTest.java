package com.riigiluup.alignment;

import com.riigiluup.AbstractIntegrationTest;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.support.EntityFactory;
import com.riigiluup.vote.IndividualVoteRepository;
import com.riigiluup.vote.VoteChoice;
import com.riigiluup.vote.VoteEvent;
import com.riigiluup.vote.VoteEventRepository;
import com.riigiluup.vote.VoteEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs {@link FactionAlignmentBackfillService} against real Postgres — the
 * "recomputeOne" path exercised by the backfill is the same code the
 * VoteEventImporter runs post-import, so this doubles as regression coverage
 * for that inline call.
 */
@Transactional
class FactionAlignmentComputerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PlenaryMemberRepository memberRepo;
    @Autowired private VoteEventRepository voteEventRepo;
    @Autowired private IndividualVoteRepository individualVoteRepo;
    @Autowired private VoteFactionAlignmentRepository alignmentRepo;
    @Autowired private FactionAlignmentBackfillService backfill;

    private PlenaryMember reform1, reform2, reform3, ekre1;
    private VoteEvent event;

    @BeforeEach
    void seed() {
        alignmentRepo.deleteAll();
        individualVoteRepo.deleteAll();
        voteEventRepo.deleteAll();
        memberRepo.deleteAll();

        reform1 = memberRepo.save(EntityFactory.member(
                "mp-r1", "R", "One", "Reformierakond", true));
        reform2 = memberRepo.save(EntityFactory.member(
                "mp-r2", "R", "Two", "Reformierakond", true));
        reform3 = memberRepo.save(EntityFactory.member(
                "mp-r3", "R", "Three", "Reformierakond", true));
        ekre1 = memberRepo.save(EntityFactory.member(
                "mp-e1", "E", "One", "EKRE", true));

        event = voteEventRepo.save(EntityFactory.voteEvent(
                "ev-A", Instant.parse("2026-01-15T10:15:00Z"), VoteEventType.OPEN));

        individualVoteRepo.saveAll(List.of(
                EntityFactory.individualVote(event, reform1, VoteChoice.FOR),
                EntityFactory.individualVote(event, reform2, VoteChoice.FOR),
                EntityFactory.individualVote(event, reform3, VoteChoice.AGAINST),
                EntityFactory.individualVote(event, ekre1, VoteChoice.AGAINST)));
    }

    @Test
    void recomputeAll_writes_alignment_row_per_faction() {
        backfill.recomputeAll();

        List<VoteFactionAlignment> rows = alignmentRepo.findByVoteEvent(event);
        assertThat(rows).hasSize(2);

        VoteFactionAlignment reform = rows.stream()
                .filter(a -> a.getFactionExternalId().equals("F-Reformierakond"))
                .findFirst().orElseThrow();
        assertThat(reform.getMajorityChoice()).isEqualTo(VoteChoice.FOR);
        assertThat(reform.getMajorityCount()).isEqualTo(2);
        assertThat(reform.getComparableCount()).isEqualTo(3);
        assertThat(reform.isHasClearMajority()).isTrue();

        VoteFactionAlignment ekre = rows.stream()
                .filter(a -> a.getFactionExternalId().equals("F-EKRE"))
                .findFirst().orElseThrow();
        assertThat(ekre.getMajorityChoice()).isEqualTo(VoteChoice.AGAINST);
        assertThat(ekre.isHasClearMajority()).isTrue();
    }

    @Test
    void tied_faction_gets_no_clear_majority() {
        // Seed has reform1=FOR, reform2=FOR, reform3=AGAINST → majority FOR.
        // Flip reform2 → AGAINST so the tally is 1 FOR / 2 AGAINST (still a
        // clear majority, but AGAINST), then also drop reform3 to AGAINST
        // and flip reform1 to AGAINST — wait, that's 3 AGAINST. Instead,
        // make it a true 1/1 split by flipping reform1 to AGAINST and
        // removing reform3 entirely.
        var iv1 = individualVoteRepo.findByVoteEventAndPlenaryMember(event, reform1).orElseThrow();
        iv1.setChoice(VoteChoice.AGAINST);
        individualVoteRepo.save(iv1);
        var iv3 = individualVoteRepo.findByVoteEventAndPlenaryMember(event, reform3).orElseThrow();
        individualVoteRepo.delete(iv3);
        // Now: reform1=AGAINST, reform2=FOR  → 1v1 tie in the Reform faction.

        backfill.recomputeAll();

        VoteFactionAlignment reform = alignmentRepo.findByVoteEvent(event).stream()
                .filter(a -> a.getFactionExternalId().equals("F-Reformierakond"))
                .findFirst().orElseThrow();
        assertThat(reform.isHasClearMajority()).isFalse();
        assertThat(reform.getMajorityChoice()).isNull();
    }
}
