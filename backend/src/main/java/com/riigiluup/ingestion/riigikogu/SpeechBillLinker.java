package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.speech.SpeechBillLink;
import com.riigiluup.speech.SpeechBillLinkRepository;
import com.riigiluup.speech.SpeechRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One-shot backfill that (re)builds speech_bill_link for speeches already ingested, by parsing
 * the draft code out of each agenda-item title. Idempotent: it clears the table and rebuilds
 * from scratch, so it stays consistent with the per-sitting links the importer writes going
 * forward. Membership itself is backfilled by the migration; this only fills the code links.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechBillLinker {

    private final SpeechRepository speechRepo;
    private final SpeechBillLinkRepository linkRepo;

    @Transactional
    public Map<String, Object> linkAll() {
        linkRepo.deleteAllInBatch(); // full rebuild — this is the sole writer during a one-shot run
        List<SpeechBillLink> batch = new ArrayList<>();
        int speechesLinked = 0;
        for (SpeechRepository.SpeechAgendaRow row : speechRepo.findAllAgendaTitles()) {
            List<AgendaDraftRef> refs = AgendaDraftRef.parse(row.getAgendaItemTitle());
            if (refs.isEmpty()) continue;
            speechesLinked++;
            for (AgendaDraftRef ref : refs) {
                batch.add(SpeechBillLink.builder()
                        .speechId(row.getId())
                        .mark(ref.mark())
                        .draftTypeCode(ref.typeCode())
                        .build());
            }
        }
        linkRepo.saveAll(batch);
        log.info("speech-bill link backfill: {} speeches linked, {} links created",
                speechesLinked, batch.size());
        return Map.of("speechesLinked", speechesLinked, "linksCreated", batch.size());
    }
}
