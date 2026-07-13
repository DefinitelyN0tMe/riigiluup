package com.politico.ingestion.riigikogu;

import com.fasterxml.jackson.databind.JsonNode;
import com.politico.legislation.LegislativeItem;
import com.politico.legislation.LegislativeItemRepository;
import com.politico.source.SourceSnapshot;
import com.politico.source.SourceSnapshotRepository;
import com.politico.vote.VoteEvent;
import com.politico.vote.VoteEventRepository;
import lombok.RequiredArgsConstructor;
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
        // For MVP simplicity we scan snapshots directly; a dedicated
        // findBySourceNameAndEntityTypeAndExternalId finder is a Phase-6 tweak.
        List<SourceSnapshot> candidates = snapshotRepo.findAll().stream()
                .filter(s -> SOURCE.equals(s.getSourceName())
                        && DETAIL_ENTITY.equals(s.getEntityType())
                        && event.getExternalId().equals(s.getExternalId()))
                .toList();
        if (candidates.isEmpty()) return false;
        JsonNode payload = candidates.get(candidates.size() - 1).getPayload();
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
