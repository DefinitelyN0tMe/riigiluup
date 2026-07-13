package com.politico.vote;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VoteEventRepository extends JpaRepository<VoteEvent, UUID> {

    Optional<VoteEvent> findBySourceNameAndExternalId(String sourceName, String externalId);

    @Query("""
        select v from VoteEvent v
        where (:from is null or v.startedAt >= :from)
          and (:to is null or v.startedAt <= :to)
          and (:type is null or v.type = :type)
        order by v.startedAt desc
        """)
    Page<VoteEvent> search(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("type") VoteEventType type,
            Pageable pageable
    );
}
