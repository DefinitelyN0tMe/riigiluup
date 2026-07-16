package com.politico.source;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SourceSnapshotRepository extends JpaRepository<SourceSnapshot, UUID> {

    Optional<SourceSnapshot> findFirstBySourceNameAndEntityTypeAndExternalIdAndPayloadHash(
            String sourceName, String entityType, String externalId, String payloadHash
    );

    Optional<SourceSnapshot> findFirstBySourceNameAndEntityTypeAndExternalIdOrderByFetchedAtDesc(
            String sourceName, String entityType, String externalId
    );

    /** True if we already stored a detail snapshot for this entity — used to skip re-fetching it. */
    boolean existsBySourceNameAndEntityTypeAndExternalId(
            String sourceName, String entityType, String externalId
    );

    /** True if we have a snapshot fetched more recently than {@code after} — a freshness gate. */
    boolean existsBySourceNameAndEntityTypeAndExternalIdAndFetchedAtAfter(
            String sourceName, String entityType, String externalId, java.time.Instant after
    );

    /**
     * Bulk delete used by {@link SnapshotRetentionJob}. Returns the number of rows removed.
     * Dependent tables (plenary_member, group, group_membership, vote_event, legislative_item)
     * have ON DELETE SET NULL on their source_snapshot_id FKs (see V8), so old detail
     * snapshots can be purged without cascading into domain data.
     */
    @Modifying
    @Transactional
    @Query("delete from SourceSnapshot s "
            + "where s.entityType in :entityTypes and s.fetchedAt < :cutoff")
    int deleteByEntityTypeInAndFetchedAtBefore(
            @org.springframework.data.repository.query.Param("entityTypes")
                    Collection<String> entityTypes,
            @org.springframework.data.repository.query.Param("cutoff") Instant cutoff
    );

    /**
     * Batched delete (one bounded transaction per call) so a large ageing cohort can't blow the
     * 30 s statement_timeout in a single statement — which would roll back and never succeed,
     * letting the table grow unbounded. The retention job loops this until it returns 0.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM source_snapshot WHERE id IN ("
            + "SELECT id FROM source_snapshot "
            + "WHERE entity_type IN (:entityTypes) AND fetched_at < :cutoff LIMIT :batch)",
            nativeQuery = true)
    int deleteBatchByEntityTypeInAndFetchedAtBefore(
            @org.springframework.data.repository.query.Param("entityTypes")
                    Collection<String> entityTypes,
            @org.springframework.data.repository.query.Param("cutoff") Instant cutoff,
            @org.springframework.data.repository.query.Param("batch") int batch
    );
}
