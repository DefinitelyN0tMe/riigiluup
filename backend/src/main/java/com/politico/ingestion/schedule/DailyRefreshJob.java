package com.politico.ingestion.schedule;

import com.politico.ingestion.riigikogu.LegislativeItemImporter;
import com.politico.ingestion.riigikogu.PlenaryMemberDetailImporter;
import com.politico.ingestion.riigikogu.PlenaryMemberImporter;
import com.politico.ingestion.riigikogu.UsergroupImporter;
import com.politico.ingestion.riigikogu.VoteBillLinker;
import com.politico.ingestion.riigikogu.VoteEventImporter;
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

    @Scheduled(cron = "${politico.schedule.daily-refresh-cron}",
               zone = "${politico.schedule.daily-refresh-zone}")
    public void runDaily() {
        if (!running.compareAndSet(false, true)) {
            log.info("Daily refresh skipped — previous run still in progress");
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
