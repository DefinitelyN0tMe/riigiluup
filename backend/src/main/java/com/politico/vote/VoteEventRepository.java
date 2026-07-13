package com.politico.vote;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
        where (cast(:from as instant) is null or v.startedAt >= :from)
          and (cast(:to as instant) is null or v.startedAt <= :to)
          and (cast(:type as string) is null or v.type = :type)
        order by v.startedAt desc
        """)
    Page<VoteEvent> search(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("type") VoteEventType type,
            Pageable pageable
    );

    /**
     * Paginated iteration over every VoteEvent by startedAt asc. Used by the alignment
     * backfill so we never hold the whole table in memory. Sort baked into the query
     * so the pageable only needs page/size (though Sort can still override).
     */
    @Query("select v from VoteEvent v order by v.startedAt asc nulls last, v.id asc")
    Slice<VoteEvent> findAllByStartedAtAsc(Pageable pageable);

    /**
     * Paginated iteration of VoteEvents that still need bill linking
     * (legislative_item still NULL). Ordered so the caller gets a stable slice window.
     */
    @Query("select v from VoteEvent v where v.legislativeItem is null "
            + "order by v.startedAt asc nulls last, v.id asc")
    Slice<VoteEvent> findUnlinkedByStartedAtAsc(Pageable pageable);
}
