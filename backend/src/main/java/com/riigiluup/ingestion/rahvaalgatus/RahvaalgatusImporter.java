package com.riigiluup.ingestion.rahvaalgatus;

import com.riigiluup.group.Group;
import com.riigiluup.group.GroupRepository;
import com.riigiluup.group.GroupType;
import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import com.riigiluup.initiative.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Full refresh of every citizen initiative. The whole corpus is a single ~284 KB CSV request,
 * so a full pass is trivially cheaper than tracking deltas.
 *
 * <p>Municipal initiatives are imported too — filtering them at read time costs nothing (the
 * CSV arrives whole regardless) and keeps the door open for a municipal view later.
 *
 * <p>An unknown phase / decision slug fails the run loudly and visibly in /admin: this is a
 * full upsert, so nothing is left half-written, and a re-run after the fix picks everything
 * up. An unknown committee slug does NOT fail — see {@link InitiativeCommittee}.
 */
@Slf4j
@Service
public class RahvaalgatusImporter {

    private static final String JOB_NAME = "initiatives.full-refresh";

    private final RahvaalgatusClient client;
    private final InitiativeRepository repo;
    private final InitiativeCommitteeLinkRepository linkRepo;
    private final GroupRepository groupRepo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;

    public RahvaalgatusImporter(
            RahvaalgatusClient client,
            InitiativeRepository repo,
            InitiativeCommitteeLinkRepository linkRepo,
            GroupRepository groupRepo,
            ImportRunLogRepository runLogRepo,
            PlatformTransactionManager txManager
    ) {
        this.client = client;
        this.repo = repo;
        this.linkRepo = linkRepo;
        this.groupRepo = groupRepo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    /** Daily at 05:40 Tallinn — after the Riigikogu passes, so committees already exist. */
    @Scheduled(cron = "0 40 5 * * *", zone = "Europe/Tallinn")
    public void scheduledRefresh() {
        try {
            runFullRefresh();
        } catch (Exception e) {
            log.warn("Scheduled rahvaalgatus refresh failed", e);
        }
    }

    public ImportRunLog runFullRefresh() {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(client.sourceName())
                .jobName(JOB_NAME)
                .startedAt(Instant.now())
                .status("RUNNING")
                .build());
        int seen = 0;
        int upserted = 0;
        try {
            String csv = client.fetchInitiativesCsv();
            List<RahvaalgatusCsvParser.Row> rows = RahvaalgatusCsvParser.parse(csv);
            seen = rows.size();
            Map<String, UUID> committeeIdsByName = activeCommitteeIdsByName();
            for (RahvaalgatusCsvParser.Row row : rows) {
                if (row.externalId() == null) continue;
                tx.executeWithoutResult(status -> upsert(row, committeeIdsByName));
                upserted++;
            }
            run.setStatus("SUCCESS");
            log.info("imported {} rahvaalgatus initiatives", upserted);
        } catch (Exception e) {
            log.error("rahvaalgatus import failed", e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
        } finally {
            run.setRecordsSeen(seen);
            run.setRecordsUpserted(upserted);
            run.setFinishedAt(Instant.now());
            runLogRepo.save(run);
        }
        return run;
    }

    /** Loaded once per run — 11 rows, but resolving per initiative would be 1141 queries. */
    private Map<String, UUID> activeCommitteeIdsByName() {
        Map<String, UUID> byName = new HashMap<>();
        for (Group g : groupRepo.findByTypeAndActiveTrueOrderByName(GroupType.STANDING_COMMITTEE)) {
            byName.put(g.getName(), g.getId());
        }
        return byName;
    }

    private void upsert(RahvaalgatusCsvParser.Row row, Map<String, UUID> committeeIdsByName) {
        Initiative i = repo.findBySourceNameAndExternalId(client.sourceName(), row.externalId())
                .orElseGet(() -> Initiative.builder()
                        .sourceName(client.sourceName())
                        .externalId(row.externalId())
                        .build());
        i.setUuid(row.uuid());
        i.setTitle(row.title());
        i.setAuthors(row.authors());
        i.setDestination(row.destination());
        i.setPhase(InitiativePhase.fromSlug(row.phase()));
        i.setPublishedAt(row.publishedAt());
        i.setSigningStartedAt(row.signingStartedAt());
        i.setSigningEndsAt(row.signingEndsAt());
        i.setSignatureCount(row.signatureCount());
        i.setLastSignedAt(row.lastSignedAt());
        i.setSentToParliamentAt(row.sentToParliamentAt());
        i.setParliamentDecision(ParliamentDecision.fromSlug(row.parliamentDecision()));
        i.setFinishedInParliamentAt(row.finishedInParliamentAt());
        i.setSentToGovernmentAt(row.sentToGovernmentAt());
        i.setFinishedInGovernmentAt(row.finishedInGovernmentAt());
        i.setImportedAt(Instant.now());
        // legislativeItemId / linkedBy / linkedAt are curated — the importer never touches them.
        Initiative saved = repo.save(i);
        reconcileCommittees(saved.getId(), row.committees(), committeeIdsByName);
    }

    /** Delete-and-recreate: at most a couple of rows per initiative, and the source is truth. */
    private void reconcileCommittees(
            Long initiativeId, List<String> slugs, Map<String, UUID> committeeIdsByName) {
        linkRepo.deleteByInitiativeId(initiativeId);
        linkRepo.flush();
        for (String slug : slugs) {
            UUID groupId = InitiativeCommittee.fromSlug(slug)
                    .map(InitiativeCommittee::committeeName)
                    .map(committeeIdsByName::get)
                    .orElse(null);
            if (groupId == null) {
                log.debug("committee slug '{}' did not resolve to an active committee", slug);
            }
            linkRepo.save(InitiativeCommitteeLink.builder()
                    .initiativeId(initiativeId)
                    .committeeSlug(slug)
                    .groupId(groupId)
                    .build());
        }
    }
}
