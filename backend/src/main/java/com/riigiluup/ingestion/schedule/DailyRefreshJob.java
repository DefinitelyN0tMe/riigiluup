package com.riigiluup.ingestion.schedule;

import com.riigiluup.ingestion.riigikogu.LegislativeItemImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberDetailImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberImporter;
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
            log.info("Votes refresh finished");
        } catch (Exception e) {
            log.error("Votes refresh failed", e);
        } finally {
            running.set(false);
        }
    }

    /**
     * Members, committees and legislation once a day — these change slowly. With legislation
     * detail change-detection and member-detail freshness gating, this no longer re-downloads the
     * whole bill catalogue or all 101 member details on every run.
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
            detailImporter.runOnce();
            LocalDate today = LocalDate.now(TALLINN);
            voteImporter.runWindow(today.minusDays(7), today);
            legislationImporter.runWindow(today.minusDays(7), today);
            voteBillLinker.linkAll();
            log.info("Daily refresh finished");
        } catch (Exception e) {
            log.error("Daily refresh failed", e);
        } finally {
            running.set(false);
        }
    }
}
