package com.politico.alignment;

import com.politico.AbstractIntegrationTest;
import com.politico.person.PlenaryMember;
import com.politico.person.PlenaryMemberRepository;
import com.politico.support.EntityFactory;
import com.politico.vote.IndividualVoteRepository;
import com.politico.vote.VoteChoice;
import com.politico.vote.VoteEvent;
import com.politico.vote.VoteEventRepository;
import com.politico.vote.VoteEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class GroupAlignmentServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PlenaryMemberRepository memberRepo;
    @Autowired private VoteEventRepository voteEventRepo;
    @Autowired private IndividualVoteRepository individualVoteRepo;
    @Autowired private VoteFactionAlignmentRepository alignmentRepo;
    @Autowired private FactionAlignmentBackfillService backfill;
    @Autowired private GroupAlignmentService service;

    private PlenaryMember target;
    private Instant windowStart;
    private Instant windowEnd;

    @BeforeEach
    void seed() {
        alignmentRepo.deleteAll();
        individualVoteRepo.deleteAll();
        voteEventRepo.deleteAll();
        memberRepo.deleteAll();

        target = memberRepo.save(EntityFactory.member(
                "mp-tgt", "Target", "MP", "Reformierakond", true));
        PlenaryMember peer1 = memberRepo.save(EntityFactory.member(
                "mp-p1", "Peer", "One", "Reformierakond", true));
        PlenaryMember peer2 = memberRepo.save(EntityFactory.member(
                "mp-p2", "Peer", "Two", "Reformierakond", true));

        Instant t0 = Instant.parse("2026-01-15T10:00:00Z");
        VoteEvent v1 = voteEventRepo.save(EntityFactory.voteEvent(
                "gv-1", t0, VoteEventType.OPEN));
        VoteEvent v2 = voteEventRepo.save(EntityFactory.voteEvent(
                "gv-2", t0.plus(1, ChronoUnit.DAYS), VoteEventType.OPEN));
        VoteEvent v3 = voteEventRepo.save(EntityFactory.voteEvent(
                "gv-3", t0.plus(2, ChronoUnit.DAYS), VoteEventType.OPEN));

        // v1: faction (peer1, peer2) FOR + target FOR  → target matches
        individualVoteRepo.saveAll(List.of(
                EntityFactory.individualVote(v1, peer1, VoteChoice.FOR),
                EntityFactory.individualVote(v1, peer2, VoteChoice.FOR),
                EntityFactory.individualVote(v1, target, VoteChoice.FOR)));
        // v2: faction FOR (peer1, peer2) + target AGAINST → deviation
        individualVoteRepo.saveAll(List.of(
                EntityFactory.individualVote(v2, peer1, VoteChoice.FOR),
                EntityFactory.individualVote(v2, peer2, VoteChoice.FOR),
                EntityFactory.individualVote(v2, target, VoteChoice.AGAINST)));
        // v3: faction AGAINST + target AGAINST → matches
        individualVoteRepo.saveAll(List.of(
                EntityFactory.individualVote(v3, peer1, VoteChoice.AGAINST),
                EntityFactory.individualVote(v3, peer2, VoteChoice.AGAINST),
                EntityFactory.individualVote(v3, target, VoteChoice.AGAINST)));

        backfill.recomputeAll();

        windowStart = t0.minus(1, ChronoUnit.DAYS);
        windowEnd = t0.plus(10, ChronoUnit.DAYS);
    }

    @Test
    void forMember_returns_two_out_of_three_matches() {
        GroupAlignmentService.Result r = service.forMember(target, windowStart, windowEnd);

        assertThat(r.eligible()).isEqualTo(3);
        assertThat(r.matches()).isEqualTo(2);
        assertThat(r.rate()).isEqualTo(2.0 / 3.0);
    }

    @Test
    void forMember_with_null_window_bounds_covers_all_votes() {
        // Regression cover: the JPQL uses `cast(:from as instant) is null`.
        GroupAlignmentService.Result r = service.forMember(target, null, null);
        assertThat(r.eligible()).isEqualTo(3);
        assertThat(r.matches()).isEqualTo(2);
    }

    @Test
    void recentDeviations_lists_only_diverging_votes() {
        var deviations = service.recentDeviations(target, 10);
        assertThat(deviations).hasSize(1);
        assertThat(deviations.get(0).getVoteEvent().getExternalId()).isEqualTo("gv-2");
    }
}
