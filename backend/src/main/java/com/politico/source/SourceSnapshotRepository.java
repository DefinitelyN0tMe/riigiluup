package com.politico.source;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SourceSnapshotRepository extends JpaRepository<SourceSnapshot, UUID> {

    Optional<SourceSnapshot> findFirstBySourceNameAndEntityTypeAndExternalIdAndPayloadHash(
            String sourceName, String entityType, String externalId, String payloadHash
    );

    Optional<SourceSnapshot> findFirstBySourceNameAndEntityTypeAndExternalIdOrderByFetchedAtDesc(
            String sourceName, String entityType, String externalId
    );
}
