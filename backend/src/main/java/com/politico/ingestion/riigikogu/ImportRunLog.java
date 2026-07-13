package com.politico.ingestion.riigikogu;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_run_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportRunLog {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    @Column(name = "job_name", nullable = false, length = 128)
    private String jobName;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "records_seen", nullable = false)
    private int recordsSeen;

    @Column(name = "records_upserted", nullable = false)
    private int recordsUpserted;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
