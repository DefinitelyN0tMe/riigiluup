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
    private final AnalyticsCacheEvictor cacheEvictor;

    /** Guards against a slow run still executing when the next 6-hourly trigger fires. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Votes every 6 hours so new roll-calls appear the same day. Cheap: the votings list is
     * date-filtered and historical votes are immutable, so only the recent window is fetched.
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
            voteImporter.runWindow(today.minusDays(7), today);
            voteBillLinker.linkAll();
            cacheEvictor.evictAll();
            log.info("Votes refresh finished");
        } catch (Exception e) {
            log.error("Votes refresh failed", e);
        } finally {
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
            voteImporter.runWindow(today.minusDays(7), today);
            legislationImporter.runWindow(today.minusDays(7), today);
            voteBillLinker.linkAll();
            // 7-day speech window: stenograms publish next day and get edited for a few
            // days after, so re-upserting a week keeps texts converged with the source.
            speechImporter.runWindow(today.minusDays(7), today);
            // Oversight: pick up new written questions/interpellations and answers (incl. late replies
            // to older questions). Cheap windowed re-scan of the document register.
            oversightImporter.refreshRecent();
            cacheEvictor.evictAll();
            log.info("Daily refresh finished");
        } catch (Exception e) {
            log.error("Daily refresh failed", e);
        } finally {
            running.set(false);
        }
    }
}
