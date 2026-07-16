package com.politico.vote;

import com.politico.person.PlenaryMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IndividualVoteRepository extends JpaRepository<IndividualVote, UUID> {

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
