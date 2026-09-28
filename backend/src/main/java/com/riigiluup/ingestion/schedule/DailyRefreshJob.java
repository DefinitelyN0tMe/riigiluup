package com.riigiluup.ingestion.schedule;

import com.riigiluup.common.AnalyticsCacheEvictor;
import com.riigiluup.ingestion.riigikogu.LegislativeItemImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberDetailImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberImporter;
import com.riigiluup.ingestion.riigikogu.SpeechImporter;
import com.riigiluup.ingestion.riigikogu.UsergroupImporter;
import com.riigiluup.ingestion.riigikogu.VoteBillLinker;
import com.riigiluup.ingestion.riigikogu.VoteEventImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyRefreshJob {

    private static final ZoneId TALLINN = ZoneId.of("Europe/Tallinn");

    private final PlenaryMemberImporter memberImporter;
    private final UsergroupImporter usergroupImporter;
    private final PlenaryMemberDetailImporter detailImporter;
    private final VoteEventImporter voteImporter;
    private final LegislativeItemImporter legislationImporter;
    private final VoteBillLinker voteBillLinker;
    private final SpeechImporter speechImporter;
    private final com.riigiluup.oversight.OversightImporter oversightImporter;
    private final com.riigiluup.ingestion.riigikogu.ImportRunLogRepository runLogRepo;
    private final AnalyticsCacheEvictor cacheEvictor;
    private final com.riigiluup.alert.TelegramAlertService alert;
    private final com.riigiluup.person.PlenaryMemberRepository memberRepo;
    private final com.riigiluup.group.GroupMembershipRepository membershipRepo;
    private final com.riigiluup.seo.IndexNowService indexNow;

    /**
     * Window start for a windowed refresh: normally {@code today - defaultDays}, but if the last
     * successful run of {@code jobName} was longer ago than that (downtime / failure), start from just
     * before that last success so the gap is re-scanned — capped at {@code maxDays} to bound cost.
     */
    private LocalDate windowStart(String jobName, LocalDate today, int defaultDays, int maxDays) {
        LocalDate def = today.minusDays(defaultDays);
        LocalDate cap = today.minusDays(maxDays);
        return runLogRepo.findFirstByJobNameAndStatusOrderByStartedAtDesc(jobName, "SUCCESS")
                .map(r -> r.getFinishedAt() == null ? null : r.getFinishedAt().atZone(TALLINN).toLocalDate().minusDays(1))
                .filter(d -> d != null && d.isBefore(def))   // only widen (older start), never narrow
                .map(d -> d.isBefore(cap) ? cap : d)          // bound the catch-up window
                .orElse(def);
    }

    /** Guards against a slow run still executing when the next 6-hourly trigger fires. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Runs one import step in isolation: an exception or a returned FAILED run log is logged and sent
     * to Telegram, and the caller carries on with the next step. Importers record most failures as a
     * FAILED row and return normally, so checking the returned status is what makes them visible.
     * PARTIAL (some records skipped, the rest landed) is logged only; the freshness watchdog covers a
     * job that stops landing data altogether.
     */
    private void step(String label, java.util.function.Supplier<Object> body) {
        try {
            Object result = body.get();
            if (result instanceof com.riigiluup.ingestion.riigikogu.ImportRunLog run) {
                if ("FAILED".equals(run.getStatus())) {
                    String err = run.getErrorMessage() == null ? "" : " — " + abbreviate(run.getErrorMessage());
                    log.error("{} failed: {}", label, run.getErrorMessage());
                    alert.send("⚠️ RiigiLuup: " + label + " ebaõnnestus" + err);
                } else if ("PARTIAL".equals(run.getStatus())) {
                    log.warn("{} partial: {}", label, run.getErrorMessage());
                }
            }
        } catch (Exception e) {
            log.error("{} failed", label, e);
            alert.send("⚠️ RiigiLuup: " + label + " ebaõnnestus — " + abbreviate(e.toString()));
        }
    }

    private static String abbreviate(String s) {
        String one = s.replaceAll("\\s+", " ").trim();
        return one.length() > 200 ? one.substring(0, 200) + "…" : one;
    }

    /**
     * Votes AND stenograms every 6 hours so new roll-calls and speeches appear the same day.
     * Cheap: the votings list is date-filtered and historical votes are immutable, so only the
     * recent window is fetched; the verbatims window is a single API call. Each step is isolated, so a
     * stenogram failure never masks or aborts the vote refresh (and vice versa).
     */
    @Scheduled(cron = "${riigiluup.schedule.votes-refresh-cron}",
               zone = "${riigiluup.schedule.daily-refresh-zone}")
    public void refreshVotes() {
        if (!running.compareAndSet(false, true)) {
            log.info("Votes refresh skipped — another refresh is in progress");
            return;
        }
        log.info("Votes refresh starting");
        java.time.Instant since = java.time.Instant.now();
        try {
            LocalDate today = LocalDate.now(TALLINN);
            step("hääletuste värskendus",
                    () -> voteImporter.runWindow(windowStart("votes.window-refresh", today, 7, 90), today));
            step("hääletuste ja eelnõude sidumine", voteBillLinker::linkAll);
            // 7-day speech window, 6-hourly: stenograms publish next day and get edited for a few
            // days after, so re-upserting a week keeps texts converged with the source, and polling
            // every 6 hours picks a freshly published stenogram up within hours, not the next morning.
            step("stenogrammide värskendus",
                    () -> speechImporter.runWindow(windowStart("speeches.window-refresh", today, 7, 60), today));
            log.info("Votes refresh finished");
        } finally {
            cacheEvictor.evictAll();
            indexNow.submitChangedSince(since);
            running.set(false);
        }
    }

    /**
     * Members, committees and legislation once a day. Legislation uses change-detection so the whole
     * bill catalogue is not re-downloaded; member detail IS force-refreshed daily (see below) so
     * faction/committee moves surface next-day, since the list feed carries no faction to diff on.
     * Every step runs in isolation: one failing source no longer skips everything after it.
     */
    @Scheduled(cron = "${riigiluup.schedule.daily-refresh-cron}",
               zone = "${riigiluup.schedule.daily-refresh-zone}")
    public void refreshDaily() {
        if (!running.compareAndSet(false, true)) {
            log.info("Daily refresh skipped — another refresh is in progress");
            return;
        }
        log.info("Daily refresh starting");
        java.time.Instant since = java.time.Instant.now();
        try {
            LocalDate today = LocalDate.now(TALLINN);
            step("komisjonide ja ühenduste värskendus", usergroupImporter::runOnce);
            step("saadikute nimekirja värskendus", memberImporter::runOnce);
            // Force a full detail refresh daily (not the 7-day freshness-gated path): the
            // /api/plenary-members list feed carries no faction, so member detail is the only
            // source of faction, committee role and the faction-history timeline. Refreshing it
            // every day means a faction departure/switch or committee change shows up the next
            // day rather than up to a week later. ~101 throttled calls, a couple of minutes.
            step("saadikute detailide värskendus", () -> detailImporter.runOnce(true));
            step("hääletuste värskendus",
                    () -> voteImporter.runWindow(windowStart("votes.window-refresh", today, 7, 90), today));
            step("eelnõude värskendus", () -> legislationImporter.runWindow(today.minusDays(7), today));
            // A new amendment does not change a bill's stage, so the change-detection window above
            // skips it; refresh amendments for all active bills daily so new proposals surface next-day.
            step("muudatusettepanekute värskendus", () -> { legislationImporter.refreshActiveBillAmendments(); return null; });
            step("hääletuste ja eelnõude sidumine", voteBillLinker::linkAll);
            // Speeches are refreshed 6-hourly in refreshVotes() (see above), not here.
            // Oversight: pick up new written questions/interpellations and answers (incl. late replies
            // to older questions). Cheap windowed re-scan of the document register.
            step("arupärimiste ja küsimuste värskendus", oversightImporter::refreshRecent);
            checkIntegrity();
            log.info("Daily refresh finished");
        } finally {
            // Also after a partial failure: whatever did land is live and worth showing and announcing.
            cacheEvictor.evictAll();
            indexNow.submitChangedSince(since);
            running.set(false);
        }
    }

    /** The Riigikogu has 101 seats. */
    private static final long CHAMBER_SIZE = 101;

    /**
     * Cheap invariants on the current composition, checked after every daily refresh. A silent drift
     * here (an ended mandate never deactivated, a former MP still listed as a committee member) is
     * exactly the kind of error a reader or a journalist spots first, so it goes to Telegram the same
     * day instead of waiting to be noticed on the site. A count off by one can also be a real, brief
     * transition (a seat vacant until the substitute is sworn in): the alert says "check", not "bug".
     */
    void checkIntegrity() {
        try {
            long active = memberRepo.countByActiveTrue();
            long staleSeats = membershipRepo.countActiveHeldByInactiveMembers();
            if (active != CHAMBER_SIZE || staleSeats > 0) {
                String msg = "⚠️ RiigiLuup andmete kontroll: aktiivseid saadikuid " + active + " (peaks olema "
                        + CHAMBER_SIZE + "), endiste saadikute aktiivseid liikmesusi " + staleSeats + ". Kontrolli.";
                log.warn(msg);
                alert.send(msg);
            } else {
                log.info("integrity check ok: {} active MPs, no stale memberships", active);
            }
        } catch (Exception e) {
            log.warn("integrity check failed to run: {}", e.toString());
        }
    }
}
