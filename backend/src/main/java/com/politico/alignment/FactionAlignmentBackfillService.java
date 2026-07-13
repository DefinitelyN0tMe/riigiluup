package com.politico.alignment;

import com.politico.vote.IndividualVote;
import com.politico.vote.IndividualVoteRepository;
import com.politico.vote.VoteEvent;
import com.politico.vote.VoteEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FactionAlignmentBackfillService {

    private final VoteEventRepository voteEventRepo;
    private final IndividualVoteRepository individualVoteRepo;
    private final VoteFactionAlignmentRepository alignmentRepo;
    private final FactionAlignmentComputer computer;
    private final TransactionTemplate tx;

    public FactionAlignmentBackfillService(
            VoteEventRepository voteEventRepo,
            IndividualVoteRepository individualVoteRepo,
            VoteFactionAlignmentRepository alignmentRepo,
            FactionAlignmentComputer computer,
            PlatformTransactionManager txManager
    ) {
        this.voteEventRepo = voteEventRepo;
        this.individualVoteRepo = individualVoteRepo;
        this.alignmentRepo = alignmentRepo;
        this.computer = computer;
        this.tx = new TransactionTemplate(txManager);
    }

    /** Iterate every VoteEvent and recompute faction alignment. Returns event count processed. */
    public int recomputeAll() {
        int processed = 0;
        int page = 0;
        Slice<VoteEvent> slice;
        do {
            slice = voteEventRepo.findAllByStartedAtAsc(PageRequest.of(page, 500));
            for (VoteEvent ev : slice.getContent()) {
                try {
                    tx.executeWithoutResult(status -> recomputeOne(ev));
                    processed++;
                } catch (Exception e) {
                    log.warn("alignment recompute failed for {} ({}): {}",
                            ev.getId(), ev.getDescription(), e.toString());
                }
            }
            page++;
        } while (slice.hasNext());
        return processed;
    }

    private void recomputeOne(VoteEvent event) {
        List<IndividualVote> all = individualVoteRepo
                .findByVoteEventOrderByFactionNameAscPlenaryMember_LastNameAsc(event);
        Map<String, List<IndividualVote>> byFaction = new LinkedHashMap<>();
        for (IndividualVote iv : all) {
            String key = iv.getFactionExternalId();
            if (key == null) continue;
            byFaction.computeIfAbsent(key, k -> new ArrayList<>()).add(iv);
        }
        alignmentRepo.deleteByVoteEvent(event);
        for (Map.Entry<String, List<IndividualVote>> e : byFaction.entrySet()) {
            FactionAlignmentComputer.Result r = computer.compute(e.getValue());
            alignmentRepo.save(VoteFactionAlignment.builder()
                    .voteEvent(event)
                    .factionExternalId(e.getKey())
                    .majorityChoice(r.majorityChoice())
                    .majorityCount(r.majorityCount())
                    .comparableCount(r.comparableCount())
                    .hasClearMajority(r.hasClearMajority())
                    .computedAt(Instant.now())
                    .build());
        }
    }
}
