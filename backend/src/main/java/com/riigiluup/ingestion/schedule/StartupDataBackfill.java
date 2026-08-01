package com.riigiluup.ingestion.schedule;

import com.riigiluup.election.ElectionResultRepository;
import com.riigiluup.election.ElectionResultsImporter;
import com.riigiluup.ingestion.riigikogu.SpeechBillLinker;
import com.riigiluup.speech.SpeechBillLinkRepository;
import com.riigiluup.speech.SpeechRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * One-time, self-gating data load for the two features the routine refresh does not cover:
 * <ul>
 *   <li>speech to bill links for speeches ingested before that feature existed (the daily job
 *       only links the 7-day window it re-imports), and</li>
 *   <li>the EP/KOV electoral footprint (the daily job refreshes only RK-adjacent data).</li>
 * </ul>
 * Each step runs only when its own table is empty, so this loads the data on the first boot after
 * these features deploy and no-ops on every boot afterwards. It runs after the app is ready, on a
 * daemon thread, and swallows failures — it must never delay or break startup (the autoheal
 * sidecar watches the health endpoint). External-API failures simply retry on the next boot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDataBackfill {

    private final SpeechRepository speechRepo;
    private final SpeechBillLinkRepository speechBillLinkRepo;
    private final SpeechBillLinker speechBillLinker;
    private final ElectionResultRepository electionRepo;
    private final ElectionResultsImporter electionResultsImporter;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        Thread t = new Thread(this::backfillOnce, "startup-data-backfill");
        t.setDaemon(true); // never keep the JVM alive; readiness is not blocked on it
        t.start();
    }

    private void backfillOnce() {
        linkSpeechesIfNeeded();
        loadCampaignsIfNeeded();
    }

    private void linkSpeechesIfNeeded() {
        try {
            if (speechRepo.count() > 0 && speechBillLinkRepo.count() == 0) {
                log.info("Startup backfill: building speech->bill links for already-ingested speeches");
                speechBillLinker.linkAll();
            }
        } catch (Exception e) {
            log.warn("Startup speech->bill backfill failed (retries next boot): {}", e.toString());
        }
    }

    private void loadCampaignsIfNeeded() {
        // Gate per code, not all-or-nothing: a code that failed or was unpublished at an earlier
        // boot is retried on the next boot, independent of the codes that already loaded.
        for (String code : ElectionResultsImporter.CAMPAIGN_CODES) {
            try {
                if (electionRepo.countByElectionCode(code) == 0) {
                    log.info("Startup backfill: importing election {}", code);
                    electionResultsImporter.importCampaign(code);
                }
            } catch (Exception e) {
                log.warn("Startup campaign import {} failed (retries next boot): {}", code, e.toString());
            }
        }
    }
}
