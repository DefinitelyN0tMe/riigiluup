package com.riigiluup.admin;

import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.LegislativeItemImporter;
import com.riigiluup.ingestion.riigikogu.VoteBillLinker;
import com.riigiluup.ingestion.riigikogu.VoteEventImporter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
     * and stops cleanly. Retries once on optimistic-lock collision with the loop's own
     * progress save; if it still loses the race we log and move on — the loop will pick
     * up the CANCELLED status on its next iteration anyway.
     */
    public Optional<BackfillRun> cancel(UUID runId) {
        for (int attempt = 0; attempt < 2; attempt++) {
            Optional<BackfillRun> found = runRepo.findById(runId);
            if (found.isEmpty()) return Optional.empty();
            BackfillRun run = found.get();
            if (!STATUS_RUNNING.equals(run.getStatus())) {
                return Optional.of(run);
            }
            run.setStatus(STATUS_CANCELLED);
            run.setEndedAt(Instant.now());
            try {
                return Optional.of(runRepo.save(run));
            } catch (ObjectOptimisticLockingFailureException e) {
                log.debug("cancel({}) lost optimistic lock, retrying (attempt {})",
                        runId, attempt);
            }
        }
        log.warn("cancel({}) still losing optimistic lock — loop will observe status "
                + "flip on next progress save", runId);
        return runRepo.findById(runId);
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

        // Bills: a single all-drafts pass at the top. Riigikogu's /api/volumes/drafts
        // ignores startDate/endDate for filtering, so calling it per-window is a
        // pointless multiplication (see LegislativeItemImporter#runAllDrafts).
        if (doBills) {
            try {
                log.info("backfill {} running one-shot all-drafts import", runId);
                ImportRunLog r = legislationImporter.runAllDrafts();
                BackfillRun snap = runRepo.findById(runId).orElse(null);
                if (snap == null) return;
                if (STATUS_CANCELLED.equals(snap.getStatus())) {
                    log.info("backfill {} cancelled after all-drafts pass", runId);
                    snap.setEndedAt(Instant.now());
                    runRepo.save(snap);
                    return;
                }
                snap.setBillsImported(snap.getBillsImported() + safeUpserted(r));
                runRepo.save(snap);
            } catch (Exception e) {
                log.error("backfill {} all-drafts pass failed: {}", runId, e.toString());
                // Non-fatal: proceed with votes anyway; ops can re-run bills alone.
            }
        }

        // If only BILLS was requested, we're done — no window loop for votes.
        if (!doVotes) {
            BackfillRun finished = runRepo.findById(runId).orElse(null);
            if (finished == null) return;
            if (STATUS_CANCELLED.equals(finished.getStatus())) return;
            finished.setStatus(STATUS_COMPLETED);
            finished.setEndedAt(Instant.now());
            runRepo.save(finished);
            log.info("backfill {} completed (BILLS-only)", runId);
            return;
        }

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
                // Bills are handled in one shot before the loop — see runAllDrafts above.
                if (doVotes) {
                    ImportRunLog r = voteImporter.runWindow(cursor, windowEnd);
                    run.setVotesImported(run.getVotesImported() + safeUpserted(r));
                    // runWindow swallows its own exceptions and reports via status, so inspect it
                    // rather than trusting the (rarely-thrown) try/catch — otherwise a fully broken
                    // upstream marches through every window as "ok" and the run finishes COMPLETED
                    // with empty windows that are never revisited.
                    if ("FAILED".equals(r.getStatus())) {
                        windowOk = false;
                        run.setErrorMessage(cursor + ".." + windowEnd + ": vote window import returned FAILED");
                    }
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
            try {
                runRepo.save(run);
            } catch (ObjectOptimisticLockingFailureException e) {
                // A concurrent cancel() bumped the version out from under us.
                // Next loop iteration will re-read and see CANCELLED, so just log.
                log.info("progress save for run {} lost optimistic lock — likely a "
                        + "concurrent cancel; will observe status on next iteration", runId);
            }

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
