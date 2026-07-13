package com.politico.ingestion.riigikogu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.politico.legislation.LegislativeItem;
import com.politico.legislation.LegislativeItemRepository;
import com.politico.legislation.LegislativeItemTopic;
import com.politico.legislation.LegislativeItemTopicRepository;
import com.politico.legislation.LegislativeSponsorship;
import com.politico.legislation.LegislativeSponsorshipRepository;
import com.politico.legislation.LegislativeStage;
import com.politico.legislation.LegislativeStageRepository;
import com.politico.legislation.Topic;
import com.politico.legislation.TopicRepository;
import com.politico.person.PlenaryMember;
import com.politico.person.PlenaryMemberRepository;
import com.politico.source.ProcessingStatus;
import com.politico.source.SourceSnapshot;
import com.politico.source.SourceSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
public class LegislativeItemImporter {

    private static final String JOB_NAME = "legislation.window-refresh";
    private static final String ENTITY_LIST = "draft-list";
    private static final String ENTITY_DETAIL = "draft-detail";
    private static final String TOPIC_SOURCE = "eurovoc";

    private final RiigikoguClient client;
    private final LegislativeItemMapper mapper;
    private final LegislativeStageFlattener stageFlattener;
    private final SponsorClassifier sponsorClassifier;
    private final ObjectMapper json;
    private final LegislativeItemRepository itemRepo;
    private final LegislativeStageRepository stageRepo;
    private final LegislativeSponsorshipRepository sponsorshipRepo;
    private final TopicRepository topicRepo;
    private final LegislativeItemTopicRepository itemTopicRepo;
    private final PlenaryMemberRepository memberRepo;
    private final SourceSnapshotRepository snapshotRepo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;

    public LegislativeItemImporter(
            RiigikoguClient client,
            LegislativeItemMapper mapper,
            LegislativeStageFlattener stageFlattener,
            SponsorClassifier sponsorClassifier,
            ObjectMapper json,
            LegislativeItemRepository itemRepo,
            LegislativeStageRepository stageRepo,
            LegislativeSponsorshipRepository sponsorshipRepo,
            TopicRepository topicRepo,
            LegislativeItemTopicRepository itemTopicRepo,
            PlenaryMemberRepository memberRepo,
            SourceSnapshotRepository snapshotRepo,
            ImportRunLogRepository runLogRepo,
            PlatformTransactionManager txManager
    ) {
        this.client = client;
        this.mapper = mapper;
        this.stageFlattener = stageFlattener;
        this.sponsorClassifier = sponsorClassifier;
        this.json = json;
        this.itemRepo = itemRepo;
        this.stageRepo = stageRepo;
        this.sponsorshipRepo = sponsorshipRepo;
        this.topicRepo = topicRepo;
        this.itemTopicRepo = itemTopicRepo;
        this.memberRepo = memberRepo;
        this.snapshotRepo = snapshotRepo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    public ImportRunLog runWindow(LocalDate from, LocalDate to) {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(client.sourceName()).jobName(JOB_NAME)
                .startedAt(Instant.now()).status("RUNNING").build());
        int seen = 0, upserted = 0;
        try {
            LocalDate cursor = from;
            while (!cursor.isAfter(to)) {
                LocalDate windowEnd = cursor.plusDays(6);
                if (windowEnd.isAfter(to)) windowEnd = to;
                log.info("fetching drafts window {}..{}", cursor, windowEnd);
                DraftListDto page = client.fetchDraftsInWindow(cursor, windowEnd);
                client.throttle();
                if (page != null && page._embedded() != null
                        && page._embedded().content() != null) {
                    for (DraftListDto.DraftListEntry entry : page._embedded().content()) {
                        seen++;
                        try {
                            tx.executeWithoutResult(status -> upsertOne(entry));
                            upserted++;
                        } catch (Exception e) {
                            log.warn("failed draft {} ({}): {}",
                                    entry.uuid(), entry.title(), e.toString());
                        }
                        client.throttle();
                    }
                }
                cursor = windowEnd.plusDays(1);
            }
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("legislation window import failed", e);
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

    private void upsertOne(DraftListDto.DraftListEntry entry) {
        LegislativeItem existing = itemRepo
                .findBySourceNameAndExternalId(client.sourceName(), entry.uuid())
                .orElse(null);

        SourceSnapshot listSnap = snapshotFor(ENTITY_LIST, entry.uuid(), entry);
        if (existing == null) {
            existing = mapper.fromListEntry(entry);
            existing.setSourceSnapshot(listSnap);
            existing = itemRepo.save(existing);
        } else {
            mapper.applyDetail(existing, toDetailShapeFromEntry(entry));
            existing.setSourceSnapshot(listSnap);
        }

        DraftDetailDto detail;
        try {
            detail = client.fetchDraftDetail(entry.uuid());
        } catch (Exception e) {
            log.warn("detail fetch failed for {}: {}", entry.uuid(), e.toString());
            listSnap.setProcessingStatus(ProcessingStatus.PROCESSED);
            return;
        }
        SourceSnapshot detailSnap = snapshotFor(ENTITY_DETAIL, detail.uuid(), detail);
        mapper.applyDetail(existing, detail);
        existing.setSourceSnapshot(detailSnap);

        reconcileStages(existing, detail);
        reconcileSponsorships(existing, detail);
        reconcileTopics(existing, detail);
        detailSnap.setProcessingStatus(ProcessingStatus.PROCESSED);
    }

    private void reconcileStages(LegislativeItem item, DraftDetailDto d) {
        stageRepo.deleteByLegislativeItem(item);
        for (LegislativeStageFlattener.Flat flat : stageFlattener.flatten(d.readings())) {
            stageRepo.save(LegislativeStage.builder()
                    .legislativeItem(item)
                    .readingCode(flat.readingCode())
                    .statusCode(flat.statusCode())
                    .occurredAt(flat.occurredAt())
                    .sequence(flat.sequence())
                    .build());
        }
    }

    private void reconcileSponsorships(LegislativeItem item, DraftDetailDto d) {
        sponsorshipRepo.deleteByLegislativeItem(item);
        if (d.initiators() == null) return;
        for (DraftDetailDto.Initiator ini : d.initiators()) {
            var kind = sponsorClassifier.classify(ini);
            PlenaryMember linkedMp = null;
            if (kind == com.politico.legislation.SponsorKind.PLENARY_MEMBER && ini.uuid() != null) {
                linkedMp = memberRepo
                        .findBySourceNameAndExternalId(client.sourceName(), ini.uuid())
                        .orElse(null);
            }
            sponsorshipRepo.save(LegislativeSponsorship.builder()
                    .legislativeItem(item)
                    .sponsorKind(kind)
                    .plenaryMember(linkedMp)
                    .externalId(ini.uuid())
                    .displayName(ini.name())
                    .build());
        }
    }

    private void reconcileTopics(LegislativeItem item, DraftDetailDto d) {
        itemTopicRepo.deleteByLegislativeItem(item);
        if (d.descriptors() == null) return;
        for (DraftDetailDto.Descriptor desc : d.descriptors()) {
            if (desc.edid() == null) continue;
            Topic topic = topicRepo
                    .findBySourceNameAndEdid(TOPIC_SOURCE, desc.edid())
                    .orElseGet(() -> topicRepo.save(Topic.builder()
                            .sourceName(TOPIC_SOURCE)
                            .edid(desc.edid())
                            .text(desc.text() == null ? "(no label)" : desc.text())
                            .build()));
            itemTopicRepo.save(LegislativeItemTopic.builder()
                    .legislativeItem(item)
                    .topic(topic)
                    .build());
        }
    }

    /** Wrap a list entry as a synthetic detail for the mapper's applyDetail path. */
    private static DraftDetailDto toDetailShapeFromEntry(DraftListDto.DraftListEntry e) {
        return new DraftDetailDto(
                e.uuid(), e.title(), null, e.mark(), e.membership(), e.draftTypeCode(),
                e.activeDraftStage(), e.activeDraftStatus(), null,
                e.initiated(), null, e.amendmentsDeadline(),
                e.leadingCommittee(), List.of(), List.of(), List.of()
        );
    }

    private SourceSnapshot snapshotFor(String entity, String externalId, Object payload) {
        String str;
        try { str = json.writeValueAsString(payload); }
        catch (Exception e) { throw new IllegalStateException(e); }
        String hash = sha256(str);
        return snapshotRepo
                .findFirstBySourceNameAndEntityTypeAndExternalIdAndPayloadHash(
                        client.sourceName(), entity, externalId, hash)
                .orElseGet(() -> snapshotRepo.save(SourceSnapshot.builder()
                        .sourceName(client.sourceName()).entityType(entity)
                        .externalId(externalId).payload(json.valueToTree(payload))
                        .payloadHash(hash)
                        .sourceUrl("https://api.riigikogu.ee/api/volumes/drafts/" + externalId)
                        .fetchedAt(Instant.now())
                        .processingStatus(ProcessingStatus.PENDING).build()));
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes()));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
