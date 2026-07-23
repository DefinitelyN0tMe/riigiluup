package com.riigiluup.ingestion.riigikogu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ImportRunLogRepository extends JpaRepository<ImportRunLog, UUID> {
    Optional<ImportRunLog> findFirstBySourceNameAndJobNameOrderByStartedAtDesc(
            String sourceName, String jobName
    );

    /** Most recent successful (or partial) import finish time — drives the "last sync" stat. */
    @Query("select max(r.finishedAt) from ImportRunLog r where r.status in ('SUCCESS', 'PARTIAL')")
    Optional<Instant> findLastSuccessfulSyncAt();
}
