package com.riigiluup.source;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.riigiluup.AbstractIntegrationTest;
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

        // Stale details that have a newer snapshot of the same entity: purged.
        repo.save(snapshot("voting-detail", "vote-a", longAgo));
        repo.save(snapshot("voting-detail", "vote-a", recent));
        repo.save(snapshot("draft-detail", "draft-a", longAgo));
        repo.save(snapshot("draft-detail", "draft-a", longAgo.plus(1, ChronoUnit.DAYS)));

        // Stale but the only (latest) snapshot of its entity: kept, change detection depends on it.
        repo.save(snapshot("plenary-member-detail", "stale-mp", longAgo));

        // Stale summary: retained (only detail types are purged).
        repo.save(snapshot("voting-summary", "stale-summary", longAgo));

        assertThat(repo.count()).isEqualTo(6);

        job.purgeOldDetailSnapshots();

        assertThat(repo.findAll())
                .extracting(x -> x.getExternalId() + "@" + (x.getFetchedAt().isBefore(recent.minusSeconds(1)) ? "old" : "new"))
                .containsExactlyInAnyOrder("vote-a@new", "draft-a@old", "stale-mp@old", "stale-summary@old");
        assertThat(repo.count()).isEqualTo(4);
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

        int deleted = repo.deleteByEntityTypeInAndFetchedAtBefore(
                SnapshotRetentionJob.DETAIL_ENTITY_TYPES, cutoff);

        assertThat(deleted).isEqualTo(2);
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
