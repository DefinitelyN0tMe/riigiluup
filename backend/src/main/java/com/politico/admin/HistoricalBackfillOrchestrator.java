package com.politico.admin;

import com.politico.ingestion.riigikogu.ImportRunLog;
import com.politico.ingestion.riigikogu.LegislativeItemImporter;
import com.politico.ingestion.riigikogu.VoteBillLinker;
import com.politico.ingestion.riigikogu.VoteEventImporter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Long-running, resumable historical backfill.
 *
 * <p>Complementary to {@link AdminBackfillController}, which runs a single window inline.
 * This orchestrator walks a multi-year range in 30-day sliding windows on a background
 * thread, persisting progress to {@code backfill_run} so ops can poll/cancel it.
 */
@Slf4j
@Service
public class HistoricalBackfillOrchestrator {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    /** Length of each backfill window in days (importers internally sub-window per week). */
    private static final int WINDOW_DAYS = 30;
    /** Breathing-room sleep between windows in milliseconds. */
    private static final long INTER_WINDOW_SLEEP_MS = 100L;
    /** Fail-fast threshold — abort the whole run if this many windows fail back-to-back. */
    private static final int MAX_CONSECUTIVE_WINDOW_FAILURES = 5;

    private final BackfillRunRepository runRepo;
    private final LegislativeItemImporter legislationImporter;
    private final VoteEventImporter voteImporter;
    private final VoteBillLinker voteBillLinker;
    private final ExecutorService executor;

    public HistoricalBackfillOrchestrator(
            BackfillRunRepository runRepo,
            LegislativeItemImporter legislationImporter,
            VoteEventImporter voteImporter,
            VoteBillLinker voteBillLinker
    ) {
        this.runRepo = runRepo;
        this.legislationImporter = legislationImporter;
        this.voteImporter = voteImporter;
        this.voteBillLinker = voteBillLinker;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "historical-backfill");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Kick off a background backfill. Rejects (returns empty) if another RUNNING run exists.
     */
    public synchronized Optional<BackfillRun> startAsync(
            LocalDate from, LocalDate to, Set<String> kinds
    ) {
        if (runRepo.findFirstByStatusOrderByStartedAtDesc(STATUS_RUNNING).isPresent()) {
            return Optional.empty();
        }
        Set<String> normalized = normalizeKinds(kinds);
        int totalWindows = countWindows(from, to);
        BackfillRun run = BackfillRun.builder()
                .id(UUID.randomUUID())
                .startedAt(Instant.now())
                .fromDate(from)
                .toDate(to)
                .currentWindowStart(from)
                .kinds(String.join(",", normalized))
                .status(STATUS_RUNNING)
                .billsImported(0)
                .votesImported(0)
                .windowsCompleted(0)
                .windowsTotal(totalWindows)
                .build();
        run = runRepo.save(run);
        UUID runId = run.getId();
        executor.submit(() -> runLoop(runId, from, to, normalized));
        return Optional.of(run);
    }

    public Optional<BackfillRun> getProgress(UUID runId) {
        return runRepo.findById(runId);
    }

    public Optional<BackfillRun> getLatest() {
        return runRepo.findFirstByOrderByStartedAtDesc();
    }

    /**
     * Flip the run to CANCELLED. The background loop notices at the next window boundary
     * and stops cleanly.
     */
    public Optional<BackfillRun> cancel(UUID runId) {
        return runRepo.findById(runId).map(run -> {
            if (STATUS_RUNNING.equals(run.getStatus())) {
                run.setStatus(STATUS_CANCELLED);
                run.setEndedAt(Instant.now());
                return runRepo.save(run);
            }
            return run;
        });
    }

    /**
     * On startup, any run left in RUNNING from a previous JVM is an orphan — mark it FAILED.
     * We deliberately do NOT auto-resume; ops must start a new run pointing at
     * {@code current_window_start} if they want to continue.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void resume() {
        runRepo.findFirstByStatusOrderByStartedAtDesc(STATUS_RUNNING).ifPresent(orphan -> {
            log.warn("orphan RUNNING backfill run {} detected — marking FAILED (aborted by restart). "
                            + "Resume from {} manually if desired.",
                    orphan.getId(), orphan.getCurrentWindowStart());
            orphan.setStatus(STATUS_FAILED);
            orphan.setErrorMessage("aborted by restart");
            orphan.setEndedAt(Instant.now());
            runRepo.save(orphan);
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------------

    private void runLoop(UUID runId, LocalDate from, LocalDate to, Set<String> kinds) {
        log.info("historical backfill {} starting from={} to={} kinds={}", runId, from, to, kinds);
        int consecutiveFailures = 0;
        LocalDate cursor = from;
        boolean doBills = kinds.contains("BILLS");
        boolean doVotes = kinds.contains("VOTES");

        while (!cursor.isAfter(to)) {
            // Re-fetch each iteration so a CANCELLED status flip is observed promptly.
            BackfillRun run = runRepo.findById(runId).orElse(null);
            if (run == null) {
                log.warn("backfill run {} disappeared mid-flight, stopping", runId);
                return;
            }
            if (STATUS_CANCELLED.equals(run.getStatus())) {
                log.info("backfill run {} cancelled at cursor={}", runId, cursor);
                run.setEndedAt(Instant.now());
                runRepo.save(run);
                return;
            }

            LocalDate windowEnd = cursor.plusDays(WINDOW_DAYS - 1L);
            if (windowEnd.isAfter(to)) windowEnd = to;
            log.info("backfill {} window {}..{} ({}/{} done)",
                    runId, cursor, windowEnd, run.getWindowsCompleted(), run.getWindowsTotal());

            boolean windowOk = true;
            try {
                if (doBills) {
                    ImportRunLog r = legislationImporter.runWindow(cursor, windowEnd);
                    run.setBillsImported(run.getBillsImported() + safeUpserted(r));
                }
                if (doVotes) {
                    ImportRunLog r = voteImporter.runWindow(cursor, windowEnd);
                    run.setVotesImported(run.getVotesImported() + safeUpserted(r));
                }
            } catch (Exception e) {
                log.warn("backfill {} window {}..{} failed: {}", runId, cursor, windowEnd, e.toString());
                run.setErrorMessage(cursor + ".." + windowEnd + ": " + e);
                windowOk = false;
            }

            if (windowOk) {
                consecutiveFailures = 0;
            } else {
                consecutiveFailures++;
                if (consecutiveFailures >= MAX_CONSECUTIVE_WINDOW_FAILURES) {
                    log.error("backfill {} aborting: {} consecutive window failures",
                            runId, consecutiveFailures);
                    run.setStatus(STATUS_FAILED);
                    run.setErrorMessage("aborted after " + consecutiveFailures
                            + " consecutive window failures; last error: " + run.getErrorMessage());
                    run.setEndedAt(Instant.now());
                    runRepo.save(run);
                    return;
                }
            }

            cursor = windowEnd.plusDays(1);
            run.setCurrentWindowStart(cursor);
            run.setWindowsCompleted(run.getWindowsCompleted() + 1);
            runRepo.save(run);

            sleepQuietly(INTER_WINDOW_SLEEP_MS);
        }

        // Link votes to bills once at the end (only meaningful when both kinds ran).
        if (doBills && doVotes) {
            try {
                int linked = voteBillLinker.linkAll();
                log.info("backfill {} linked {} votes to bills", runId, linked);
            } catch (Exception e) {
                log.warn("backfill {} vote-bill link step failed: {}", runId, e.toString());
            }
        }

        BackfillRun run = runRepo.findById(runId).orElse(null);
        if (run == null) return;
        // If it was cancelled during the very last window we already returned above; treat this as done.
        run.setStatus(STATUS_COMPLETED);
        run.setEndedAt(Instant.now());
        runRepo.save(run);
        log.info("historical backfill {} COMPLETED bills={} votes={} windows={}/{}",
                runId, run.getBillsImported(), run.getVotesImported(),
                run.getWindowsCompleted(), run.getWindowsTotal());
    }

    private static int safeUpserted(ImportRunLog r) {
        return r == null ? 0 : r.getRecordsUpserted();
    }

    private static int countWindows(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days <= 0) return 0;
        return (int) ((days + WINDOW_DAYS - 1) / WINDOW_DAYS);
    }

    private static Set<String> normalizeKinds(Set<String> raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw != null) {
            for (String k : raw) {
                if (k == null) continue;
                String up = k.trim().toUpperCase();
                if (up.equals("BILLS") || up.equals("VOTES")) out.add(up);
            }
        }
        if (out.isEmpty()) {
            out.add("BILLS");
            out.add("VOTES");
        }
        return out;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
