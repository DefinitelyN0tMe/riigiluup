package com.riigiluup.ingestion.schedule;

import com.riigiluup.alert.TelegramAlertService;
import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportFreshnessWatchdogTest {

    private static final Instant NOW = Instant.parse("2026-09-29T05:00:00Z");

    private static ImportRunLog run(String job, String status, Instant startedAt, String err) {
        return ImportRunLog.builder().jobName(job).status(status).startedAt(startedAt).errorMessage(err).build();
    }

    @Test
    void flagsOnlyJobsThatHaveNotLandedDataWithinTheirSchedule() {
        ImportRunLogRepository repo = mock(ImportRunLogRepository.class);
        // Default: every job landed an hour ago.
        when(repo.findFirstByJobNameAndStatusInOrderByStartedAtDesc(anyString(), any()))
                .thenAnswer(inv -> Optional.of(run(inv.getArgument(0), "SUCCESS", NOW.minus(Duration.ofHours(1)), null)));
        when(repo.findFirstByJobNameOrderByStartedAtDesc(anyString()))
                .thenAnswer(inv -> Optional.of(run(inv.getArgument(0), "SUCCESS", NOW.minus(Duration.ofHours(1)), null)));

        // Bills: last good run 8 days ago, failing every night since (the September 2026 case).
        when(repo.findFirstByJobNameAndStatusInOrderByStartedAtDesc(eq("legislation.window-refresh"), any()))
                .thenReturn(Optional.of(run("legislation.window-refresh", "SUCCESS", NOW.minus(Duration.ofDays(8)), null)));
        when(repo.findFirstByJobNameOrderByStartedAtDesc("legislation.window-refresh"))
                .thenReturn(Optional.of(run("legislation.window-refresh", "FAILED", NOW.minus(Duration.ofHours(1)),
                        "404 : \"Requested data not found\"")));
        // Monthly finance import 20 days ago: within its 35-day window, so fine.
        when(repo.findFirstByJobNameAndStatusInOrderByStartedAtDesc(eq("party-finance.full-refresh"), any()))
                .thenReturn(Optional.of(run("party-finance.full-refresh", "SUCCESS", NOW.minus(Duration.ofDays(20)), null)));
        // Never succeeded at all.
        when(repo.findFirstByJobNameAndStatusInOrderByStartedAtDesc(eq("oversight.refresh"), any()))
                .thenReturn(Optional.empty());

        List<String> problems = new ImportFreshnessWatchdog(repo, mock(TelegramAlertService.class)).findProblems(NOW);

        assertThat(problems).hasSize(2);
        assertThat(problems.get(0)).startsWith("• legislation.window-refresh: viimati õnnestus 192 h tagasi")
                .contains("(viimane: FAILED, 404 : \"Requested data not found\")");
        assertThat(problems.get(1)).isEqualTo("• oversight.refresh: pole kordagi õnnestunud");
    }
}
