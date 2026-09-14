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
     * Votes AND stenograms every 6 hours so new roll-calls and speeches appear the same day.
     * Cheap: the votings list is date-filtered and historical votes are immutable, so only the
     * recent window is fetched; the verbatims window is a single API call. Speeches run in their
     * own try/catch so a stenogram failure never masks or aborts the vote refresh (and vice versa).
     */
    @Scheduled(cron = "${riigiluup.schedule.votes-refresh-cron}",
               zone = "${riigiluup.schedule.daily-refresh-zone}")
    public void refreshVotes() {
        if (!running.compareAndSet(false, true)) {
            log.info("Votes refresh skipped — another refresh is in progress");
            return;
        }
        log.info("Votes refresh starting");
        try {
            LocalDate today = LocalDate.now(TALLINN);
            voteImporter.runWindow(windowStart("votes.window-refresh", today, 7, 90), today);
            voteBillLinker.linkAll();
            log.info("Votes refresh finished");
        } catch (Exception e) {
            log.error("Votes refresh failed", e);
            alert.send("⚠️ RiigiLuup: hääletuste värskendus ebaõnnestus — " + e);
        }
        try {
            // 7-day speech window, 6-hourly: stenograms publish next day and get edited for a few
            // days after, so re-upserting a week keeps texts converged with the source, and polling
            // every 6 hours picks a freshly published stenogram up within hours, not the next morning.
            LocalDate today = LocalDate.now(TALLINN);
            speechImporter.runWindow(windowStart("speeches.window-refresh", today, 7, 60), today);
            log.info("Speeches refresh finished");
        } catch (Exception e) {
            log.error("Speeches refresh failed", e);
            alert.send("⚠️ RiigiLuup: stenogrammide värskendus ebaõnnestus — " + e);
        } finally {
            cacheEvictor.evictAll();
            running.set(false);
        }
    }

    /**
     * Members, committees and legislation once a day. Legislation uses change-detection so the whole
     * bill catalogue is not re-downloaded; member detail IS force-refreshed daily (see below) so
     * faction/committee moves surface next-day, since the list feed carries no faction to diff on.
     */
    @Scheduled(cron = "${riigiluup.schedule.daily-refresh-cron}",
               zone = "${riigiluup.schedule.daily-refresh-zone}")
    public void refreshDaily() {
        if (!running.compareAndSet(false, true)) {
            log.info("Daily refresh skipped — another refresh is in progress");
            return;
        }
        log.info("Daily refresh starting");
        try {
            usergroupImporter.runOnce();
            memberImporter.runOnce();
            // Force a full detail refresh daily (not the 7-day freshness-gated path): the
            // /api/plenary-members list feed carries no faction, so member detail is the only
            // source of faction, committee role and the faction-history timeline. Refreshing it
            // every day means a faction departure/switch or committee change shows up the next
            // day rather than up to a week later. ~101 throttled calls, a couple of minutes.
            detailImporter.runOnce(true);
            LocalDate today = LocalDate.now(TALLINN);
            voteImporter.runWindow(windowStart("votes.window-refresh", today, 7, 90), today);
            legislationImporter.runWindow(today.minusDays(7), today);
            // A new amendment does not change a bill's stage, so the change-detection window above
            // skips it; refresh amendments for all active bills daily so new proposals surface next-day.
            legislationImporter.refreshActiveBillAmendments();
            voteBillLinker.linkAll();
            // Speeches are refreshed 6-hourly in refreshVotes() (see above), not here.
            // Oversight: pick up new written questions/interpellations and answers (incl. late replies
            // to older questions). Cheap windowed re-scan of the document register.
            oversightImporter.refreshRecent();
            cacheEvictor.evictAll();
            log.info("Daily refresh finished");
        } catch (Exception e) {
            log.error("Daily refresh failed", e);
            alert.send("⚠️ RiigiLuup: igapäevane värskendus ebaõnnestus — " + e);
        } finally {
            running.set(false);
        }
    }
}
