package com.politico.ingestion.riigikogu;

import com.fasterxml.jackson.databind.JsonNode;
import com.politico.legislation.LegislativeItem;
import com.politico.legislation.LegislativeItemRepository;
import com.politico.source.SourceSnapshot;
import com.politico.source.SourceSnapshotRepository;
import com.politico.vote.VoteEvent;
import com.politico.vote.VoteEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

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
        List<VoteEvent> events = voteEventRepo.findAll();
        int linked = 0;
        for (VoteEvent ev : events) {
            if (ev.getLegislativeItem() != null) continue;
            try {
                if (Boolean.TRUE.equals(tx.execute(status -> linkOne(ev)))) linked++;
            } catch (Exception e) {
                log.warn("link failed for vote_event {}: {}", ev.getId(), e.toString());
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
