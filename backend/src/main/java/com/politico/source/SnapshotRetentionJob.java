package com.politico.source;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Nightly cleanup of stale detail snapshots so {@code source_snapshot} doesn't grow
 * unbounded. Summary snapshots (voting-list, legislation-list, plenary-members-list)
 * are intentionally retained — they're small relative to detail payloads and are the
 * cheapest way to prove a re-run wouldn't discover anything new.
 *
 * <p>Dependent domain rows keep the deleted snapshot id as NULL thanks to
 * {@code ON DELETE SET NULL} on every FK (see V8__snapshot_fk_indexes.sql).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SnapshotRetentionJob {

    static final List<String> DETAIL_ENTITY_TYPES = List.of(
            "voting-detail", "draft-detail", "plenary-member-detail");
    static final int RETENTION_DAYS = 180;

    private final SourceSnapshotRepository repo;

    /** Detail snapshots older than 180 days go away nightly at 04:00 Europe/Tallinn. */
    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Tallinn")
    public void purgeOldDetailSnapshots() {
        Instant threshold = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        long deleted = repo.deleteByEntityTypeInAndFetchedAtBefore(
                DETAIL_ENTITY_TYPES, threshold);
        log.info("snapshot retention purged {} old detail rows (cutoff={})",
                deleted, threshold);
    }
}
