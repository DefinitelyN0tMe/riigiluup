package com.riigiluup.ingestion.schedule;

import com.riigiluup.ingestion.wikidata.WikidataImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Refreshes Wikidata-sourced MP data (party membership history, Q-IDs, bio enrichment) every two
 * days. Wikidata was previously loaded only by the one-time historical backfill / admin endpoint, so
 * party memberships and Q-IDs went stale and new MPs got none. Wikidata is a separate source, so this
 * runs independently of the Riigikogu daily refresh and never competes with its throttled client.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikidataRefreshJob {

    private final WikidataImporter wikidataImporter;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${riigiluup.schedule.wikidata-refresh-cron}",
               zone = "${riigiluup.schedule.daily-refresh-zone}")
    public void refresh() {
        if (!running.compareAndSet(false, true)) {
            log.info("Wikidata refresh skipped — a previous run is still in progress");
            return;
        }
        log.info("Wikidata refresh starting");
        try {
            wikidataImporter.runOnce();
            log.info("Wikidata refresh finished");
        } catch (Exception e) {
            log.error("Wikidata refresh failed", e);
        } finally {
            running.set(false);
        }
    }
}
