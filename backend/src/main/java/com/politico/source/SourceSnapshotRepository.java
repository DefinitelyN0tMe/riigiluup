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
    long deleteByEntityTypeInAndFetchedAtBefore(
            @org.springframework.data.repository.query.Param("entityTypes")
                    Collection<String> entityTypes,
            @org.springframework.data.repository.query.Param("cutoff") Instant cutoff
    );
}
