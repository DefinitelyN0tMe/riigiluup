package com.riigiluup.ingestion.riigiteataja;

import com.riigiluup.legislation.LegislativeItem;
import com.riigiluup.legislation.LegislativeItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Links adopted acts to Riigi Teataja. Candidates come entirely from our own DB — the
 * publication date is the AVALDATUD_RIIGITEATAJAS stage the bill importer already stores —
 * so each candidate costs exactly one RT search call. Only an unambiguous hit (exactly one
 * result on that day with that title) is stored; everything else stays unlinked. Newest
 * publications first, so the daily batch keeps up while the backfill trickles.
 */
@Slf4j
@Service
public class RtLinker {

    private final RiigiTeatajaClient rtClient;
    private final LegislativeItemRepository itemRepo;
    private final TransactionTemplate tx;

    public RtLinker(RiigiTeatajaClient rtClient,
                    LegislativeItemRepository itemRepo,
                    PlatformTransactionManager txManager) {
        this.rtClient = rtClient;
        this.itemRepo = itemRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    /** Daily small batch — new publications get linked the next morning. */
    @Scheduled(cron = "0 20 5 * * *", zone = "Europe/Tallinn")
    public void scheduledLink() {
        try {
            Map<String, Integer> r = linkBatch(50);
            log.info("RT link daily batch: {}", r);
        } catch (Exception e) {
            log.warn("Scheduled RT linking failed", e);
        }
    }

    /** Returns linked/ambiguous counts. Safe to re-run: only NULL rt_act_id rows are picked. */
    public Map<String, Integer> linkBatch(int limit) {
        List<Object[]> candidates = itemRepo.findRtLinkCandidates(Math.max(1, limit));
        int linked = 0;
        int unmatched = 0;
        for (Object[] row : candidates) {
            UUID itemId = (UUID) row[0];
            String title = (String) row[1];
            LocalDate published = ((java.sql.Date) row[2]).toLocalDate();
            var actId = rtClient.findActId(title, published);
            if (actId.isPresent()) {
                tx.executeWithoutResult(status -> {
                    LegislativeItem item = itemRepo.findById(itemId).orElse(null);
                    if (item == null) return;
                    item.setRtActId(actId.get());
                    item.setRtPublished(published);
                    itemRepo.save(item);
                });
                linked++;
            } else {
                unmatched++;
                log.info("RT link unmatched: '{}' @ {}", title, published);
            }
        }
        return Map.of("candidates", candidates.size(), "linked", linked, "unmatched", unmatched);
    }
}
