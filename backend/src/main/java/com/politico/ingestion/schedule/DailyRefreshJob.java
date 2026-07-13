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

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyRefreshJob {

    private final PlenaryMemberImporter memberImporter;
    private final UsergroupImporter usergroupImporter;
    private final PlenaryMemberDetailImporter detailImporter;
    private final VoteEventImporter voteImporter;
    private final LegislativeItemImporter legislationImporter;
    private final VoteBillLinker voteBillLinker;

    @Scheduled(cron = "${politico.schedule.daily-refresh-cron}",
               zone = "${politico.schedule.daily-refresh-zone}")
    public void runDaily() {
        log.info("Daily refresh starting");
        try {
            usergroupImporter.runOnce();
            memberImporter.runOnce();
            detailImporter.runOnce();
            LocalDate today = LocalDate.now();
            voteImporter.runWindow(today.minusDays(7), today);
            legislationImporter.runWindow(today.minusDays(7), today);
            voteBillLinker.linkAll();
            log.info("Daily refresh finished");
        } catch (Exception e) {
            log.error("Daily refresh failed", e);
        }
    }
}
