package com.politico.ingestion.schedule;

import com.politico.ingestion.riigikogu.PlenaryMemberDetailImporter;
import com.politico.ingestion.riigikogu.PlenaryMemberImporter;
import com.politico.ingestion.riigikogu.UsergroupImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyRefreshJob {

    private final PlenaryMemberImporter memberImporter;
    private final UsergroupImporter usergroupImporter;
    private final PlenaryMemberDetailImporter detailImporter;

    @Scheduled(cron = "${politico.schedule.daily-refresh-cron}",
               zone = "${politico.schedule.daily-refresh-zone}")
    public void runDaily() {
        log.info("Daily refresh starting");
        try {
            usergroupImporter.runOnce();
            memberImporter.runOnce();
            detailImporter.runOnce();
            log.info("Daily refresh finished");
        } catch (Exception e) {
            log.error("Daily refresh failed", e);
        }
    }
}
