package com.riigiluup.alignment;

import com.riigiluup.vote.VoteEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteFactionAlignmentRepository
        extends JpaRepository<VoteFactionAlignment, UUID> {

    Optional<VoteFactionAlignment> findByVoteEventAndFactionExternalId(
            VoteEvent event, String factionExternalId);

    List<VoteFactionAlignment> findByVoteEvent(VoteEvent event);

    List<VoteFactionAlignment> findByVoteEventIn(Collection<VoteEvent> events);

    @Modifying
    @Query("delete from VoteFactionAlignment a where a.voteEvent = :event")
    void deleteByVoteEvent(@Param("event") VoteEvent event);

    /**
     * Pairwise agreement between two factions: over vote events where BOTH factions had a clear
     * majority, how many times their majority choices matched (same) vs differed (diff).
     * Returns a single row [same, diff].
     */
    @Query(value = """
        SELECT count(*) FILTER (WHERE a.majority_choice = b.majority_choice) AS same_count,
               count(*) FILTER (WHERE a.majority_choice <> b.majority_choice) AS diff_count
        FROM vote_faction_alignment a
        JOIN vote_faction_alignment b ON b.vote_event_id = a.vote_event_id
        WHERE a.faction_external_id = :left AND b.faction_external_id = :right
          AND a.has_clear_majority AND b.has_clear_majority
        """, nativeQuery = true)
    List<Object[]> pairwiseCounts(@Param("left") String left, @Param("right") String right);

    /** Recent vote events where the two factions' majorities differed:
     *  row = [id, description, started_at, left_choice, right_choice], newest first. */
    @Query(value = """
        SELECT ve.id, ve.description, ve.started_at, a.majority_choice, b.majority_choice
        FROM vote_faction_alignment a
        JOIN vote_faction_alignment b ON b.vote_event_id = a.vote_event_id
        JOIN vote_event ve ON ve.id = a.vote_event_id
        WHERE a.faction_external_id = :left AND b.faction_external_id = :right
          AND a.has_clear_majority AND b.has_clear_majority
          AND a.majority_choice <> b.majority_choice
        ORDER BY ve.started_at DESC NULLS LAST
        LIMIT :lim
        """, nativeQuery = true)
    List<Object[]> recentDisagreements(
            @Param("left") String left, @Param("right") String right, @Param("lim") int lim);
}
