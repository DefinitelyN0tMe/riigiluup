package com.riigiluup.alignment;

import com.riigiluup.vote.VoteEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteFactionAlignmentRepository
        extends JpaRepository<VoteFactionAlignment, UUID> {

    Optional<VoteFactionAlignment> findByVoteEventAndFactionExternalId(
            VoteEvent event, String factionExternalId);

    List<VoteFactionAlignment> findByVoteEvent(VoteEvent event);

    @Modifying
    @Query("delete from VoteFactionAlignment a where a.voteEvent = :event")
    void deleteByVoteEvent(@Param("event") VoteEvent event);
}
