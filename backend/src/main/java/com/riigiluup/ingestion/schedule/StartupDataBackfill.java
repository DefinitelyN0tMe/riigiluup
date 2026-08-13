package com.riigiluup.ingestion.schedule;

import com.riigiluup.election.ElectionResultRepository;
import com.riigiluup.election.ElectionResultsImporter;
import com.riigiluup.election.HistoricalElectionImporter;
import com.riigiluup.ingestion.riigikogu.LegislativeItemImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberDetailImporter;
import com.riigiluup.ingestion.riigikogu.SpeechBillLinker;
import com.riigiluup.legislation.BillAmendmentRepository;
import com.riigiluup.group.GroupMembershipRepository;
import com.riigiluup.group.GroupType;
import com.riigiluup.person.MpFactionMembershipRepository;
import com.riigiluup.person.MpPressActivityRepository;
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
    private final HistoricalElectionImporter historicalElectionImporter;
    private final MpFactionMembershipRepository factionHistoryRepo;
    private final MpPressActivityRepository pressRepo;
    private final GroupMembershipRepository groupMembershipRepo;
    private final BillAmendmentRepository amendmentRepo;
    private final LegislativeItemImporter legislationImporter;
    private final com.riigiluup.oversight.OversightImporter oversightImporter;
    private final PlenaryMemberDetailImporter detailImporter;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        Thread t = new Thread(this::backfillOnce, "startup-data-backfill");
        t.setDaemon(true); // never keep the JVM alive; readiness is not blocked on it
        t.start();
    }

    private void backfillOnce() {
        linkSpeechesIfNeeded();
        loadCampaignsIfNeeded();
        loadHistoricalIfNeeded();
        loadFactionHistoryIfNeeded();
        loadPressActivityIfNeeded();
        loadAuxGroupMembershipsIfNeeded();
        loadAmendmentsIfNeeded();
        loadOversightIfNeeded();
    }

    private void loadOversightIfNeeded() {
        try {
            // Self-gated on a SUCCESS run-log, so an interrupted backfill resumes next boot.
            oversightImporter.backfillCurrentTermIfNeeded();
        } catch (Exception e) {
            log.warn("Startup oversight backfill failed (retries next boot): {}", e.toString());
        }
    }

    private void loadAmendmentsIfNeeded() {
        try {
            if (amendmentRepo.count() == 0) {
                log.info("Startup backfill: backfilling bill amendments for active bills");
                legislationImporter.refreshActiveBillAmendments();
            }
        } catch (Exception e) {
            log.warn("Startup amendment backfill failed (retries next boot): {}", e.toString());
        }
    }

    private void loadAuxGroupMembershipsIfNeeded() {
        try {
            // Friendship/support/delegation memberships are written by the detail import alongside
            // committees; backfill them once when none exist yet (first boot after this feature).
            if (groupMembershipRepo.countActiveByType(GroupType.BILATERAL_GROUP) == 0) {
                log.info("Startup backfill: forcing a detail refresh to backfill MP group memberships");
                detailImporter.runOnce(true);
            }
        } catch (Exception e) {
            log.warn("Startup group-membership backfill failed (retries next boot): {}", e.toString());
        }
    }

    private void loadPressActivityIfNeeded() {
        try {
            if (pressRepo.count() == 0) {
                log.info("Startup backfill: forcing a detail refresh to backfill MP press activity");
                // Same forced detail path as faction history: press is written by the detail import,
                // so one forced run on the first boot after this feature deploys populates it now.
                detailImporter.runOnce(true);
            }
        } catch (Exception e) {
            log.warn("Startup press-activity backfill failed (retries next boot): {}", e.toString());
        }
    }

    private void loadFactionHistoryIfNeeded() {
        try {
            if (factionHistoryRepo.count() == 0) {
                log.info("Startup backfill: forcing a detail refresh to backfill MP faction history");
                // force=true bypasses the 7-day freshness window so every member's timeline is
                // populated now (and any just-happened faction change is picked up immediately).
                detailImporter.runOnce(true);
            }
        } catch (Exception e) {
            log.warn("Startup faction-history backfill failed (retries next boot): {}", e.toString());
        }
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

    private void loadHistoricalIfNeeded() {
        try {
            if (electionRepo.countByHistoricalTrue() == 0) {
                log.info("Startup backfill: importing pre-2023 historical electoral history (Mölder dataset)");
                historicalElectionImporter.importHistorical();
            }
        } catch (Exception e) {
            log.warn("Startup historical election import failed (retries next boot): {}", e.toString());
        }
    }
}
