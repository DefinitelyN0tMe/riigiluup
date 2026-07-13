package com.politico.source;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.politico.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the retention flow end-to-end against real PostgreSQL: purge deletes only
 * detail rows past the retention cutoff and leaves summary rows / recent detail rows
 * alone.
 */
class SnapshotRetentionJobIntegrationTest extends AbstractIntegrationTest {

    @Autowired private SourceSnapshotRepository repo;
    @Autowired private SnapshotRetentionJob job;

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    @Test
    void purge_removes_only_stale_detail_rows() {
        Instant longAgo = Instant.now().minus(365, ChronoUnit.DAYS); // > 180
        Instant recent = Instant.now().minus(10, ChronoUnit.DAYS);   // < 180

        // Stale details of each retained kind — should be purged.
        repo.save(snapshot("voting-detail", "stale-vote", longAgo));
        repo.save(snapshot("draft-detail", "stale-draft", longAgo));
        repo.save(snapshot("plenary-member-detail", "stale-mp", longAgo));

        // Recent detail — retained.
        repo.save(snapshot("voting-detail", "recent-vote", recent));

        // Stale summary — retained (only detail types are purged).
        repo.save(snapshot("voting-summary", "stale-summary", longAgo));

        assertThat(repo.count()).isEqualTo(5);

        job.purgeOldDetailSnapshots();

        // Two survivors: recent-vote (detail but young) + stale-summary (wrong type).
        assertThat(repo.count()).isEqualTo(2);
        assertThat(repo.findAll())
                .extracting(SourceSnapshot::getExternalId)
                .containsExactlyInAnyOrder("recent-vote", "stale-summary");
    }

    @Test
    @Transactional
    void repo_bulk_delete_returns_row_count() {
        Instant longAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant cutoff = Instant.now().minus(SnapshotRetentionJob.RETENTION_DAYS,
                ChronoUnit.DAYS);
        repo.save(snapshot("voting-detail", "old-1", longAgo));
        repo.save(snapshot("voting-detail", "old-2", longAgo));
        repo.saveAndFlush(snapshot("voting-detail", "fresh", Instant.now()));

        long deleted = repo.deleteByEntityTypeInAndFetchedAtBefore(
                SnapshotRetentionJob.DETAIL_ENTITY_TYPES, cutoff);

        assertThat(deleted).isEqualTo(2L);
    }

    private static SourceSnapshot snapshot(String entityType, String externalId,
                                           Instant fetchedAt) {
        return SourceSnapshot.builder()
                .sourceName("riigikogu")
                .entityType(entityType)
                .externalId(externalId)
                .payload(JsonNodeFactory.instance.objectNode().put("k", "v"))
                .payloadHash(UUID.randomUUID().toString())
                .fetchedAt(fetchedAt)
                .processingStatus(ProcessingStatus.PROCESSED)
                .build();
    }
}
