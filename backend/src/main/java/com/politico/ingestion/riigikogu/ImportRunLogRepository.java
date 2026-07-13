package com.politico.ingestion.riigikogu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImportRunLogRepository extends JpaRepository<ImportRunLog, UUID> {
    Optional<ImportRunLog> findFirstBySourceNameAndJobNameOrderByStartedAtDesc(
            String sourceName, String jobName
    );
}
