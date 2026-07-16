package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.databind.JsonNode;
import com.riigiluup.legislation.LegislativeItem;
import com.riigiluup.legislation.LegislativeItemRepository;
import com.riigiluup.source.SourceSnapshot;
import com.riigiluup.source.SourceSnapshotRepository;
import com.riigiluup.vote.VoteEvent;
import com.riigiluup.vote.VoteEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class VoteBillLinker {

    private static final String SOURCE = "riigikogu";
    private static final String DETAIL_ENTITY = "voting-detail";

    private final VoteEventRepository voteEventRepo;
    private final LegislativeItemRepository itemRepo;
    private final SourceSnapshotRepository snapshotRepo;
    private final TransactionTemplate tx;

    public VoteBillLinker(
            VoteEventRepository voteEventRepo,
            LegislativeItemRepository itemRepo,
            SourceSnapshotRepository snapshotRepo,
            PlatformTransactionManager txManager
    ) {
        this.voteEventRepo = voteEventRepo;
        this.itemRepo = itemRepo;
        this.snapshotRepo = snapshotRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    public int linkAll() {
        // Note: we always fetch page=0 because linkOne() either sets legislativeItem
        // (removing the row from the unlinked slice) or leaves it null. In the "still
        // null after we tried" case the next page=0 fetch would loop forever, so we
        // step page forward for skipped rows and re-fetch page=0 whenever we linked
        // at least one row this pass (which shifts the offset back to zero anyway).
        int linked = 0;
        int page = 0;
        while (true) {
            Slice<VoteEvent> slice = voteEventRepo.findUnlinkedByStartedAtAsc(
                    PageRequest.of(page, 500));
            if (slice.getContent().isEmpty()) break;
            int linkedThisPage = 0;
            for (VoteEvent ev : slice.getContent()) {
                try {
                    if (Boolean.TRUE.equals(tx.execute(status -> linkOne(ev)))) {
                        linked++;
                        linkedThisPage++;
                    }
                } catch (Exception e) {
                    log.warn("link failed for vote_event {}: {}", ev.getId(), e.toString());
                }
            }
            if (!slice.hasNext()) break;
            if (linkedThisPage == 0) {
                // No progress on this page; advance so we don't re-scan the same rows.
                page++;
            } else {
                // We shrunk the unlinked set; page=0 now points to the next unseen rows.
                page = 0;
            }
        }
        return linked;
    }

    private Boolean linkOne(VoteEvent event) {
        SourceSnapshot snapshot = snapshotRepo
                .findFirstBySourceNameAndEntityTypeAndExternalIdOrderByFetchedAtDesc(
                        SOURCE, DETAIL_ENTITY, event.getExternalId())
                .orElse(null);
        if (snapshot == null) return false;
        JsonNode payload = snapshot.getPayload();
        JsonNode sitting = payload.get("sitting");
        if (sitting == null) return false;
        JsonNode draft = sitting.get("draft");
        if (draft == null || draft.isNull()) return false;
        String billExternalId;
        if (draft.isTextual()) {
            billExternalId = draft.asText();
        } else if (draft.has("uuid")) {
            billExternalId = draft.get("uuid").asText();
        } else {
            return false;
        }
        LegislativeItem item = itemRepo
                .findBySourceNameAndExternalId(SOURCE, billExternalId)
                .orElse(null);
        if (item == null) return false;
        event.setLegislativeItem(item);
        voteEventRepo.save(event);
        return true;
    }
}
