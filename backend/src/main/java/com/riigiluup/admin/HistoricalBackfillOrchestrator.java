package com.riigiluup.admin;

import com.riigiluup.activity.MemberActivityImporter;
import com.riigiluup.election.ElectionResultsImporter;
import com.riigiluup.finance.PartyFinanceImporter;
import com.riigiluup.ingestion.rahvaalgatus.RahvaalgatusImporter;
import com.riigiluup.ingestion.riigikogu.GovernmentQuestionImporter;
import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.LegislativeItemImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberDetailImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberImporter;
import com.riigiluup.ingestion.riigikogu.SpeechImporter;
import com.riigiluup.ingestion.riigikogu.SponsorRelinker;
import com.riigiluup.ingestion.riigikogu.UsergroupImporter;
import com.riigiluup.ingestion.riigikogu.VoteBillLinker;
import com.riigiluup.ingestion.riigikogu.VoteEventImporter;
import com.riigiluup.ingestion.riigiteataja.RtLinker;
import com.riigiluup.ingestion.wikidata.WikidataImporter;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Long-running, resumable historical backfill — the single entry point that turns a freshly
 * migrated database into a fully populated one.
 *
 * <p>The run walks a dependency-ordered pipeline over every importer, gated by the requested
 * {@code kinds} set ({@code ALL} expands to the whole pipeline). One-shot importers that fetch
 * their full corpus regardless of date (members, wikidata, questions, finance, …) run exactly
 * once; only {@code VOTES}/{@code SPEECHES} walk the {@code from..to} range in 30-day windows,
 * which is where resumability matters. Progress — the current {@link BackfillRun#getPhase()
 * phase} and per-step counts — is persisted to {@code backfill_run} so ops can poll/cancel it.
 *
 * <p>Complementary to {@link AdminBackfillController}, which runs a single window inline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalBackfillOrchestrator {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    /**
     * Every pipeline step, in dependency order. {@code ALL} expands to exactly this list;
     * an explicit {@code kinds} set is projected onto it so execution order is deterministic
     * regardless of the order the caller listed them in. Members come first because every
     * other importer references them; linkers ({@code RT_LINKS}) come last because they need
     * the adopted acts to exist.
     */
    public static final List<String> ALL_KINDS = List.of(
            "MEMBERS", "GROUPS", "DETAILS", "ELECTIONS", "WIKIDATA", "BILLS",
            "VOTES", "SPEECHES", "QUESTIONS", "INITIATIVES", "FINANCE", "ACTIVITY", "RT_LINKS");

    /** The two kinds that walk the date range; everything else is a one-shot full-corpus import. */
    private static final Set<String> WINDOWED_KINDS = Set.of("VOTES", "SPEECHES");

    /** Length of each backfill window in days (importers internally sub-window per week). */
    private static final int WINDOW_DAYS = 30;
    /** Breathing-room sleep between windows in milliseconds. */
    private static final long INTER_WINDOW_SLEEP_MS = 100L;
    /** Fail-fast threshold — abort the whole run if this many windows fail back-to-back. */
    private static final int MAX_CONSECUTIVE_WINDOW_FAILURES = 5;
    /** RT-link drain: candidates per batch, and a hard cap on batches as a runaway backstop. */
    private static final int RT_BATCH = 500;
    private static final int RT_MAX_BATCHES = 40;

    private final BackfillRunRepository runRepo;
    private final PlenaryMemberImporter memberImporter;
    private final UsergroupImporter usergroupImporter;
    private final PlenaryMemberDetailImporter detailImporter;
    private final ElectionResultsImporter electionResultsImporter;
    private final WikidataImporter wikidataImporter;
    private final LegislativeItemImporter legislationImporter;
    private final VoteEventImporter voteImporter;
    private final SpeechImporter speechImporter;
    private final GovernmentQuestionImporter governmentQuestionImporter;
    private final RahvaalgatusImporter rahvaalgatusImporter;
    private final PartyFinanceImporter partyFinanceImporter;
    private final MemberActivityImporter memberActivityImporter;
    private final VoteBillLinker voteBillLinker;
    private final SponsorRelinker sponsorRelinker;
    private final RtLinker rtLinker;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "historical-backfill");
        t.setDaemon(true);
        return t;
    });

    /** A pipeline step that imports something and returns an upsert/row count; may throw. */
    @FunctionalInterface
    private interface Step {
        int run() throws Exception;
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
        LinkedHashSet<String> normalized = canonicalize(kinds);
        boolean windowed = normalized.stream().anyMatch(WINDOWED_KINDS::contains);
        int totalWindows = windowed ? countWindows(from, to) : 0;
        BackfillRun run = BackfillRun.builder()
                .id(UUID.randomUUID())
                .startedAt(Instant.now())
                .fromDate(from)
                .toDate(to)
                .currentWindowStart(from)
                .kinds(String.join(",", normalized))
                .status(STATUS_RUNNING)
                .phase("queued")
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
     * Flip the run to CANCELLED. The background loop notices at the next step/window boundary
     * and stops cleanly. Retries once on optimistic-lock collision with the loop's own
     * progress save; if it still loses the race we log and move on — the loop will pick
     * up the CANCELLED status on its next boundary anyway.
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

    /** Package-private for the sequencing test, which drives the pipeline synchronously. */
    void runLoop(UUID runId, LocalDate from, LocalDate to, Set<String> kinds) {
        log.info("historical backfill {} starting from={} to={} kinds={}", runId, from, to, kinds);
        Map<String, Integer> counts = new LinkedHashMap<>();

        // Phase group 1 — foundational + enrichment one-shots. Order matters: members underpin
        // everything, groups/details layer committee & faction data on top, then elections and
        // Wikidata enrich matched members. Each fetches its whole corpus regardless of from/to.
        if (!oneShot(runId, kinds, "MEMBERS", "members", counts,
                () -> safeUpserted(memberImporter.runOnce()))) return;
        if (!oneShot(runId, kinds, "GROUPS", "groups", counts,
                () -> safeUpserted(usergroupImporter.runOnce()))) return;
        if (!oneShot(runId, kinds, "DETAILS", "details", counts,
                () -> safeUpserted(detailImporter.runOnce()))) return;
        if (!oneShot(runId, kinds, "ELECTIONS", "elections", counts,
                electionResultsImporter::importRk2023)) return;
        if (!oneShot(runId, kinds, "WIKIDATA", "wikidata", counts,
                () -> safeUpserted(wikidataImporter.runOnce()))) return;

        // Bills: a single all-drafts pass. Riigikogu's /api/volumes/drafts ignores
        // startDate/endDate for filtering, so calling it per-window is a pointless
        // multiplication (see LegislativeItemImporter#runAllDrafts).
        if (kinds.contains("BILLS")) {
            if (!oneShot(runId, kinds, "BILLS", "bills", counts, () -> {
                int n = safeUpserted(legislationImporter.runAllDrafts());
                mutate(runId, r -> r.setBillsImported(r.getBillsImported() + n));
                return n;
            })) return;
        }

        // Phase group 2 — windowed historical walk for votes and speeches.
        if (kinds.stream().anyMatch(WINDOWED_KINDS::contains)) {
            if (!walkWindows(runId, from, to, kinds, counts)) return;
        }

        // Phase group 3 — full-corpus one-shots that don't depend on the window walk.
        if (!oneShot(runId, kinds, "QUESTIONS", "questions", counts,
                () -> safeUpserted(governmentQuestionImporter.runFullRefresh()))) return;
        if (!oneShot(runId, kinds, "INITIATIVES", "initiatives", counts,
                () -> safeUpserted(rahvaalgatusImporter.runFullRefresh()))) return;
        if (!oneShot(runId, kinds, "FINANCE", "finance", counts,
                partyFinanceImporter::importAll)) return;
        if (!oneShot(runId, kinds, "ACTIVITY", "activity", counts,
                memberActivityImporter::computeAll)) return;

        // Phase group 4 — linkers. Vote↔bill linking needs both sides; sponsor relink needs
        // bills; both are implicit consequences of importing those kinds, not standalone kinds.
        if (kinds.contains("BILLS") && kinds.contains("VOTES")) {
            if (!oneShot(runId, kinds, "VOTES", "voteBillLinks", counts,
                    voteBillLinker::linkAll)) return;
        }
        if (kinds.contains("BILLS")) {
            if (!oneShot(runId, kinds, "BILLS", "sponsorRelinks", counts,
                    sponsorRelinker::relinkOrphanSponsors)) return;
        }
        if (!rtLinkDrain(runId, kinds, counts)) return;

        // Finalize. A cancel during the very last step already returned above.
        BackfillRun run = runRepo.findById(runId).orElse(null);
        if (run == null) return;
        if (STATUS_CANCELLED.equals(run.getStatus())) return;
        run.setStatus(STATUS_COMPLETED);
        run.setPhase("completed");
        run.setEndedAt(Instant.now());
        saveQuietly(run);
        log.info("historical backfill {} COMPLETED kinds={} counts={}", runId, kinds, counts);
    }

    /**
     * Run a single one-shot step if its kind was requested. Returns {@code true} to keep the
     * pipeline going, {@code false} to stop (the run was cancelled or vanished). Importer
     * failures are non-fatal: they are logged, recorded, and the pipeline continues — one dead
     * upstream should not block every other source.
     */
    private boolean oneShot(UUID runId, Set<String> kinds, String kind, String label,
                            Map<String, Integer> counts, Step step) {
        if (!kinds.contains(kind)) return true;
        BackfillRun run = runRepo.findById(runId).orElse(null);
        if (run == null) return false;
        if (STATUS_CANCELLED.equals(run.getStatus())) {
            finishCancel(run);
            return false;
        }
        run.setPhase(label);
        saveQuietly(run);

        try {
            int n = step.run();
            counts.put(label, n);
            log.info("backfill {} step {}: {}", runId, label, n);
        } catch (Exception e) {
            log.error("backfill {} step {} failed: {}", runId, label, e.toString());
            counts.put(label, -1); // -1 marks a failed step in step_counts
            mutate(runId, r -> r.setErrorMessage(label + ": " + e));
        }
        persistCounts(runId, counts);
        return true;
    }

    /**
     * Walk {@code from..to} in 30-day windows, importing votes and/or speeches per window.
     * Resumable via {@code current_window_start}; aborts the whole run after
     * {@link #MAX_CONSECUTIVE_WINDOW_FAILURES} back-to-back failures so a fully broken upstream
     * does not march through every window as "ok" and finish COMPLETED with empty windows.
     */
    private boolean walkWindows(UUID runId, LocalDate from, LocalDate to,
                                Set<String> kinds, Map<String, Integer> counts) {
        boolean doVotes = kinds.contains("VOTES");
        boolean doSpeeches = kinds.contains("SPEECHES");
        int consecutiveFailures = 0;
        LocalDate cursor = from;

        while (!cursor.isAfter(to)) {
            BackfillRun run = runRepo.findById(runId).orElse(null);
            if (run == null) {
                log.warn("backfill run {} disappeared mid-flight, stopping", runId);
                return false;
            }
            if (STATUS_CANCELLED.equals(run.getStatus())) {
                log.info("backfill run {} cancelled at cursor={}", runId, cursor);
                finishCancel(run);
                return false;
            }

            LocalDate windowEnd = cursor.plusDays(WINDOW_DAYS - 1L);
            if (windowEnd.isAfter(to)) windowEnd = to;
            run.setPhase("window " + cursor + ".." + windowEnd);
            log.info("backfill {} window {}..{} ({}/{} done)",
                    runId, cursor, windowEnd, run.getWindowsCompleted(), run.getWindowsTotal());

            boolean windowOk = true;
            try {
                if (doVotes) {
                    ImportRunLog r = voteImporter.runWindow(cursor, windowEnd);
                    run.setVotesImported(run.getVotesImported() + safeUpserted(r));
                    counts.merge("votes", safeUpserted(r), Integer::sum);
                    // runWindow swallows its own exceptions and reports via status, so inspect it
                    // rather than trusting the (rarely-thrown) try/catch.
                    if ("FAILED".equals(r.getStatus())) {
                        windowOk = false;
                        run.setErrorMessage(cursor + ".." + windowEnd + ": vote window import returned FAILED");
                    }
                }
                if (doSpeeches) {
                    ImportRunLog r = speechImporter.runWindow(cursor, windowEnd);
                    counts.merge("speeches", safeUpserted(r), Integer::sum);
                    if ("FAILED".equals(r.getStatus())) {
                        windowOk = false;
                        run.setErrorMessage(cursor + ".." + windowEnd + ": speech window import returned FAILED");
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
                    run.setStepCounts(jsonCounts(counts));
                    saveQuietly(run);
                    return false;
                }
            }

            cursor = windowEnd.plusDays(1);
            run.setCurrentWindowStart(cursor);
            run.setWindowsCompleted(run.getWindowsCompleted() + 1);
            run.setStepCounts(jsonCounts(counts));
            saveQuietly(run);
            sleepQuietly(INTER_WINDOW_SLEEP_MS);
        }
        return true;
    }

    /**
     * Drain the Riigi Teataja link backlog. Each batch links the newest still-unlinked adopted
     * acts; permanently-unmatchable acts stay NULL and reappear every batch, so the loop stops
     * on the first batch that links nothing (no progress) rather than on an empty candidate set,
     * which would spin forever on that unmatchable tail. Capped as a runaway backstop.
     */
    private boolean rtLinkDrain(UUID runId, Set<String> kinds, Map<String, Integer> counts) {
        if (!kinds.contains("RT_LINKS")) return true;
        BackfillRun run = runRepo.findById(runId).orElse(null);
        if (run == null) return false;
        if (STATUS_CANCELLED.equals(run.getStatus())) {
            finishCancel(run);
            return false;
        }
        run.setPhase("rtLinks");
        saveQuietly(run);

        int totalLinked = 0;
        try {
            for (int i = 0; i < RT_MAX_BATCHES; i++) {
                BackfillRun cur = runRepo.findById(runId).orElse(null);
                if (cur == null) return false;
                if (STATUS_CANCELLED.equals(cur.getStatus())) {
                    finishCancel(cur);
                    return false;
                }
                Map<String, Integer> r = rtLinker.linkBatch(RT_BATCH);
                int linked = r.getOrDefault("linked", 0);
                totalLinked += linked;
                if (linked == 0) break; // no progress — remaining acts are unmatchable for now
            }
        } catch (Exception e) {
            log.error("backfill {} step rtLinks failed: {}", runId, e.toString());
            mutate(runId, r -> r.setErrorMessage("rtLinks: " + e));
        }
        counts.put("rtLinks", totalLinked);
        log.info("backfill {} step rtLinks: {}", runId, totalLinked);
        persistCounts(runId, counts);
        return true;
    }

    // ------------------------------------------------------------------------

    /** Re-read the run, apply a mutation, and save (swallowing optimistic-lock races). */
    private void mutate(UUID runId, java.util.function.Consumer<BackfillRun> mutation) {
        BackfillRun run = runRepo.findById(runId).orElse(null);
        if (run == null) return;
        mutation.accept(run);
        saveQuietly(run);
    }

    private void persistCounts(UUID runId, Map<String, Integer> counts) {
        mutate(runId, r -> r.setStepCounts(jsonCounts(counts)));
    }

    private void finishCancel(BackfillRun run) {
        run.setEndedAt(Instant.now());
        saveQuietly(run);
    }

    private void saveQuietly(BackfillRun run) {
        try {
            runRepo.save(run);
        } catch (ObjectOptimisticLockingFailureException e) {
            // A concurrent cancel() bumped the version out from under us. The next boundary
            // re-reads and observes CANCELLED, so losing this write is harmless.
            log.debug("progress save for run {} lost optimistic lock — concurrent cancel likely",
                    run.getId());
        }
    }

    private static int safeUpserted(ImportRunLog r) {
        return r == null ? 0 : r.getRecordsUpserted();
    }

    private static int countWindows(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days <= 0) return 0;
        return (int) ((days + WINDOW_DAYS - 1) / WINDOW_DAYS);
    }

    /**
     * Project a requested kind set onto {@link #ALL_KINDS} so execution order is deterministic.
     * {@code ALL} selects the whole pipeline; unknown tokens are dropped (the controller
     * validates and rejects them before we get here). Empty resolves to the legacy
     * bills+votes default so old callers keep working.
     */
    public static LinkedHashSet<String> canonicalize(Set<String> raw) {
        boolean all = false;
        Set<String> requested = new java.util.HashSet<>();
        if (raw != null) {
            for (String k : raw) {
                if (k == null) continue;
                String up = k.trim().toUpperCase(Locale.ROOT);
                if (up.equals("ALL")) {
                    all = true;
                } else if (ALL_KINDS.contains(up)) {
                    requested.add(up);
                }
            }
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String k : ALL_KINDS) {
            if (all || requested.contains(k)) out.add(k);
        }
        if (out.isEmpty()) {
            out.add("BILLS");
            out.add("VOTES");
        }
        return out;
    }

    /** Minimal JSON serializer for the {label: count} step map — keys are known-safe literals. */
    private static String jsonCounts(Map<String, Integer> counts) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(e.getKey()).append("\":").append(e.getValue());
            first = false;
        }
        return sb.append('}').toString();
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
