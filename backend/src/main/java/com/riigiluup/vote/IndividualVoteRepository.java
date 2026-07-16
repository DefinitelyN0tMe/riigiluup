package com.riigiluup.vote;

import com.riigiluup.person.PlenaryMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IndividualVoteRepository extends JpaRepository<IndividualVote, UUID> {

    /** Roll-call participation computed from our own ingested votes (robust, no live API call). */
    interface ParticipationAgg {
        long getTotal();
        long getParticipated();
    }

    /**
     * Aggregate a member's roll-call participation from ingested OPEN votes since {@code from}.
     * Used instead of the fragile live Riigikogu statistics passthrough so the profile metric
     * never collapses to 0/0 when the source API is rate-limited.
     */
    @Query(value = """
        SELECT count(*) AS total,
               count(*) FILTER (WHERE iv.choice IN ('FOR','AGAINST','ABSTAINED')) AS participated
        FROM individual_vote iv
        JOIN vote_event ve ON ve.id = iv.vote_event_id
        WHERE iv.plenary_member_id = :memberId
          AND ve.type = 'OPEN'
          AND ve.started_at >= :from
        """, nativeQuery = true)
    ParticipationAgg aggregateVotingParticipation(
            @Param("memberId") UUID memberId, @Param("from") Instant from);

    /** Quorum-check (kohalolekukontroll) presence from our own ingested records. */
    interface AttendanceAgg {
        long getTotal();
        long getPresent();
    }

    /**
     * How often the member registered PRESENT (KOHAL) at an attendance check (kohalolekukontroll)
     * since {@code from}. This is a stricter, per-moment presence measure than the Riigikogu
     * sitting-attendance statistic, and is computed from the same records shown under Votes.
     */
    @Query(value = """
        SELECT count(*) AS total,
               count(*) FILTER (WHERE iv.choice = 'PRESENT') AS present
        FROM individual_vote iv
        JOIN vote_event ve ON ve.id = iv.vote_event_id
        WHERE iv.plenary_member_id = :memberId
          AND ve.type = 'ATTENDANCE_CHECK'
          AND ve.started_at >= :from
        """, nativeQuery = true)
    AttendanceAgg aggregateAttendanceChecks(
            @Param("memberId") UUID memberId, @Param("from") Instant from);

    Optional<IndividualVote> findByVoteEventAndPlenaryMember(
            VoteEvent voteEvent, PlenaryMember member);

    List<IndividualVote> findByVoteEventOrderByFactionNameAscPlenaryMember_LastNameAsc(
            VoteEvent event);

    @Query("""
        select iv from IndividualVote iv
        join fetch iv.plenaryMember
        where iv.voteEvent = :event
        order by iv.factionName asc, iv.plenaryMember.lastName asc
        """)
    List<IndividualVote> findByVoteEventWithMemberOrderByFactionNameAscPlenaryMember_LastNameAsc(
            @Param("event") VoteEvent event);

    @Query("""
        select iv from IndividualVote iv
        join fetch iv.voteEvent ve
        where iv.plenaryMember = :member
        order by ve.startedAt desc, ve.id desc
        """)
    Page<IndividualVote> findByMemberChronological(
            @Param("member") PlenaryMember member, Pageable pageable);
}
