package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riigiluup.legislation.LegislativeItem;
import com.riigiluup.legislation.LegislativeItemRepository;
import com.riigiluup.legislation.LegislativeItemTopic;
import com.riigiluup.legislation.LegislativeItemTopicRepository;
import com.riigiluup.legislation.LegislativeSponsorship;
import com.riigiluup.legislation.LegislativeSponsorshipRepository;
import com.riigiluup.legislation.LegislativeStage;
import com.riigiluup.legislation.LegislativeStageRepository;
import com.riigiluup.legislation.Topic;
import com.riigiluup.legislation.TopicRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.source.ProcessingStatus;
import com.riigiluup.source.SourceSnapshot;
import com.riigiluup.source.SourceSnapshotRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
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
    private final PlenaryMemberMapper memberMapper;
    private final ObjectMapper json;
    private final LegislativeItemRepository itemRepo;
    private final LegislativeStageRepository stageRepo;
    private final com.riigiluup.legislation.BillAmendmentRepository amendmentRepo;
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
            PlenaryMemberMapper memberMapper,
            ObjectMapper json,
            LegislativeItemRepository itemRepo,
            LegislativeStageRepository stageRepo,
            com.riigiluup.legislation.BillAmendmentRepository amendmentRepo,
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
        this.memberMapper = memberMapper;
        this.json = json;
        this.itemRepo = itemRepo;
        this.stageRepo = stageRepo;
        this.amendmentRepo = amendmentRepo;
        this.sponsorshipRepo = sponsorshipRepo;
        this.topicRepo = topicRepo;
        this.itemTopicRepo = itemTopicRepo;
        this.memberRepo = memberRepo;
        this.snapshotRepo = snapshotRepo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * One-shot full refresh — paginates through /api/volumes/drafts with a very
     * wide date range in a SINGLE outer window, importing every draft the API
     * exposes. Meant for the historical backfill.
     *
     * <p>Empirical finding: Riigikogu's {@code startDate/endDate} on
     * {@code /api/volumes/drafts} do NOT actually filter the returned draft
     * catalogue — the endpoint returns every draft regardless. Iterating 92
     * separate 30-day sub-windows via {@link #runWindow} therefore re-scans the
     * same catalogue 92 times. This method does it once.
     */
    public ImportRunLog runAllDrafts() {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(client.sourceName()).jobName(JOB_NAME + ".all")
                .startedAt(Instant.now()).status("RUNNING").build());
        int seen = 0, upserted = 0;
        try {
            // Wide window so the "date" params are effectively no-ops; the API
            // ignores them for draft-listing anyway.
            LocalDate wideFrom = LocalDate.of(1990, 1, 1);
            LocalDate wideTo = LocalDate.of(2100, 12, 31);
            int[] cnt = paginateAndUpsert(wideFrom, wideTo);
            seen = cnt[0];
            upserted = cnt[1];
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("legislation full import failed", e);
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

    public ImportRunLog runWindow(LocalDate from, LocalDate to) {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(client.sourceName()).jobName(JOB_NAME)
                .startedAt(Instant.now()).status("RUNNING").build());
        int seen = 0, upserted = 0;
        try {
            // /api/volumes/drafts ignores its date params (see runAllDrafts), so a single pass over
            // the catalogue covers the whole range — sub-windowing would just re-scan the same
            // drafts N times (and, with change-detection, most are skipped anyway).
            int[] cnt = paginateAndUpsert(from, to);
            seen = cnt[0];
            upserted = cnt[1];
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

    /**
     * Pull every draft in the given date band, paginating through the
     * {@code /api/volumes/drafts} response, and upsert each. Returns {seen, upserted}.
     * Shared between {@link #runWindow} (per 7-day sub-window) and
     * {@link #runAllDrafts} (wide range).
     */
    private int[] paginateAndUpsert(LocalDate from, LocalDate to) {
        int seen = 0, upserted = 0;
        int pageNum = 0;
        int fetchSize = 100;
        int safety = 500;
        while (true) {
            log.info("fetching drafts window {}..{} page={}", from, to, pageNum);
            DraftListDto page = client.fetchDraftsInWindow(from, to, pageNum, fetchSize);
            if (page == null || page._embedded() == null
                    || page._embedded().content() == null
                    || page._embedded().content().isEmpty()) break;
            for (DraftListDto.DraftListEntry entry : page._embedded().content()) {
                seen++;
                try {
                    tx.executeWithoutResult(status -> upsertOne(entry));
                    upserted++;
                } catch (CallNotPermittedException e) {
                    throw e; // circuit breaker open → let the run fail instead of persisting stage-less bills
                } catch (Exception e) {
                    log.warn("failed draft {} ({}): {}",
                            entry.uuid(), entry.title(), e.toString());
                }
            }
            DraftListDto.Page meta = page.page();
            boolean lastPage = meta == null
                    || meta.totalPages() == null
                    || meta.number() == null
                    || meta.number() >= meta.totalPages() - 1;
            if (lastPage) break;
            pageNum++;
            if (pageNum >= safety) {
                log.warn("draft-window {}..{} exceeded safety page cap {}, stopping",
                        from, to, safety);
                break;
            }
        }
        return new int[]{seen, upserted};
    }

    private void upsertOne(DraftListDto.DraftListEntry entry) {
        LegislativeItem existing = itemRepo
                .findBySourceNameAndExternalId(client.sourceName(), entry.uuid())
                .orElse(null);

        SourceSnapshot listSnap = snapshotFor(ENTITY_LIST, entry.uuid(), entry);

        // /api/volumes/drafts ignores its date params, so the refresh sees the whole catalogue every
        // run. Re-fetching every bill's detail (one throttled call each) re-downloads thousands of
        // long-finished bills that never change. If we already have this bill, its current stage
        // and status are unchanged, and we already stored its detail, then nothing we'd fetch
        // differs from what's local — skip the detail call entirely. (Stage + status are the fields
        // applyDetail keeps current on every fetch, so a bill that advances is always re-fetched and
        // then compares equal again next run — no perpetual re-fetching.)
        boolean listUnchanged = existing != null
                && java.util.Objects.equals(existing.getActiveStageSourceCode(), entry.activeDraftStage())
                && java.util.Objects.equals(existing.getActiveStatusSourceCode(), entry.activeDraftStatus());
        if (listUnchanged
                && snapshotRepo.existsBySourceNameAndEntityTypeAndExternalId(
                        client.sourceName(), ENTITY_DETAIL, entry.uuid())) {
            existing.setSourceSnapshot(listSnap);
            listSnap.setProcessingStatus(ProcessingStatus.PROCESSED);
            return;
        }

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
        } catch (CallNotPermittedException e) {
            throw e; // circuit-broken upstream → fail the run instead of persisting stage-less bills
        } catch (Exception e) {
            log.warn("detail fetch failed for {}: {}", entry.uuid(), e.toString());
            listSnap.setProcessingStatus(ProcessingStatus.PROCESSED);
            return;
        }
        SourceSnapshot detailSnap = snapshotFor(ENTITY_DETAIL, detail.uuid(), detail);
        mapper.applyDetail(existing, detail);
        existing.setSourceSnapshot(detailSnap);
        // Set status before the reconcile* calls: their @Modifying(clearAutomatically=true) deletes
        // detach the persistence context, so a status change made afterwards would be lost.
        detailSnap.setProcessingStatus(ProcessingStatus.PROCESSED);

        reconcileStages(existing, detail);
        reconcileSponsorships(existing, detail);
        reconcileTopics(existing, detail);
        reconcileAmendments(existing, detail);
    }

    /**
     * Refresh amendments for every bill still in proceeding. The daily change-detection path skips the
     * detail fetch for bills whose stage/status is unchanged, but a new amendment does NOT change a
     * bill's stage — so without this, a new amendment on a stage-static active bill would not surface
     * until the bill next advances. Run daily (and once on startup) it keeps amendments current within
     * a day. Bounded to in-proceeding phases (where amendments are proposed); concluded bills are
     * static and fill in as the daily window re-touches them. One throttled detail call per active
     * bill, each in its own transaction. Idempotent (full-replace per bill).
     */
    public void refreshActiveBillAmendments() {
        java.util.List<java.util.UUID> ids = itemRepo.findIdsByPhaseIn(java.util.List.of(
                com.riigiluup.legislation.LegislationPhase.SUBMITTED,
                com.riigiluup.legislation.LegislationPhase.IN_COMMITTEE,
                com.riigiluup.legislation.LegislationPhase.IN_READINGS));
        log.info("amendment backfill: {} active bills to fetch", ids.size());
        for (java.util.UUID id : ids) {
            try {
                tx.executeWithoutResult(status -> {
                    LegislativeItem item = itemRepo.findById(id).orElse(null);
                    if (item == null) return;
                    DraftDetailDto detail = client.fetchDraftDetail(item.getExternalId());
                    reconcileAmendments(item, detail);
                });
            } catch (Exception e) {
                log.warn("amendment backfill failed for {}: {}", id, e.toString());
            }
        }
    }

    private void reconcileAmendments(LegislativeItem item, DraftDetailDto d) {
        // Guard the null detail BEFORE the delete: the backfill path passes the fetched detail
        // straight in, and a null/empty body would otherwise NPE after the rows were already deleted.
        if (d == null || d.amendments() == null) return;
        amendmentRepo.deleteByLegislativeItem(item);
        int seq = 0;
        for (DraftDetailDto.Amendment a : d.amendments()) {
            if (a == null || a.title() == null || a.title().isBlank()) continue;
            DraftDetailDto.FileRef file = firstPublicFile(a.files());
            amendmentRepo.save(com.riigiluup.legislation.BillAmendment.builder()
                    .legislativeItem(item)
                    .externalId(a.uuid())
                    .title(a.title())
                    .reference(truncate(a.reference(), 160))
                    .fileUuid(file == null ? null : file.uuid())
                    .fileName(truncate(file == null ? null : file.fileName(), 512))
                    .sequence(seq++)
                    .importedAt(Instant.now())
                    .build());
        }
    }

    /** The first PUBLIC file of an amendment (the downloadable text), or null if none is public. */
    private static DraftDetailDto.FileRef firstPublicFile(List<DraftDetailDto.FileRef> files) {
        if (files == null) return null;
        for (DraftDetailDto.FileRef f : files) {
            if (f != null && f.uuid() != null
                    && (f.accessRestrictionType() == null || "PUBLIC".equals(f.accessRestrictionType()))) {
                return f;
            }
        }
        return null;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
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
            if (kind == com.riigiluup.legislation.SponsorKind.PLENARY_MEMBER && ini.uuid() != null) {
                linkedMp = memberRepo
                        .findBySourceNameAndExternalId(client.sourceName(), ini.uuid())
                        .orElseGet(() -> materialiseHistoricalSponsor(ini));
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

    /**
     * Create a stub {@link PlenaryMember} for a bill sponsor that isn't in the current
     * mandate roster. Bills reach back further than Riigikogu's active-members list, so
     * without this, historical sponsorships lose their MP link and drop out of the
     * co-sponsorship graph and the per-MP "bills sponsored" list.
     *
     * <p>Initiator payload doesn't carry faction info, so the stub has no faction —
     * that gets filled in later if the same UUID appears as a voter (which does carry it)
     * or on the next full members refresh.
     */
    private PlenaryMember materialiseHistoricalSponsor(DraftDetailDto.Initiator ini) {
        if (ini.uuid() == null) return null;
        PlenaryMember stub = memberMapper.stubFromVoter(ini.uuid(), ini.name(), null, null);
        if (memberRepo.findBySlug(stub.getSlug()).isPresent()) {
            stub.setSlug(stub.getSlug() + "-" + ini.uuid().substring(0, 8));
        }
        try {
            return memberRepo.save(stub);
        } catch (Exception e) {
            log.warn("failed to materialise historical sponsor {} ({}): {}",
                    ini.uuid(), ini.name(), e.toString());
            return null;
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
                e.leadingCommittee(), List.of(), List.of(), List.of(), List.of()
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
