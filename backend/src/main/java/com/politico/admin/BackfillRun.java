package com.politico.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "backfill_run")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BackfillRun {

    @Id
    private UUID id;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "current_window_start", nullable = false)
    private LocalDate currentWindowStart;

    @Column(name = "kinds", nullable = false, columnDefinition = "text")
    private String kinds;

    @Column(name = "status", nullable = false, columnDefinition = "text")
    private String status;

    @Column(name = "bills_imported", nullable = false)
    private int billsImported;

    @Column(name = "votes_imported", nullable = false)
    private int votesImported;

    @Column(name = "windows_completed", nullable = false)
    private int windowsCompleted;

    @Column(name = "windows_total", nullable = false)
    private int windowsTotal;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
