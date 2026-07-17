package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.speech.Speech;
import com.riigiluup.speech.SpeechRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Imports full speech texts from plenary verbatim records. Windows are chunked to ~2 weeks
 * per request (one verbatims call returns the whole window as one payload). Each sitting is
 * replaced wholesale in its own transaction — the source has no per-event key (its event
 * uuid identifies the speaker) and stenograms are edited after publication, so delete +
 * reinsert per sitting is the only way to stay converged with the source.
 */
@Slf4j
@Service
public class SpeechImporter {

    private static final String JOB_NAME = "speeches.window-refresh";
    private static final int CHUNK_DAYS = 14;

    private final RiigikoguClient client;
    private final SpeechMapper mapper;
    private final SpeechRepository speechRepo;
    private final PlenaryMemberRepository memberRepo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;

    public SpeechImporter(
            RiigikoguClient client,
            SpeechMapper mapper,
            SpeechRepository speechRepo,
            PlenaryMemberRepository memberRepo,
            ImportRunLogRepository runLogRepo,
            PlatformTransactionManager txManager
    ) {
        this.client = client;
        this.mapper = mapper;
        this.speechRepo = speechRepo;
        this.memberRepo = memberRepo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    public ImportRunLog runWindow(LocalDate from, LocalDate to) {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(client.sourceName())
                .jobName(JOB_NAME)
                .startedAt(Instant.now())
                .status("RUNNING")
                .build());
        int seenSpeeches = 0;
        int upserted = 0;
        int failedSittings = 0;
        try {
            Roster roster = loadRoster();
            LocalDate cursor = from;
            while (!cursor.isAfter(to)) {
                LocalDate windowEnd = cursor.plusDays(CHUNK_DAYS - 1);
                if (windowEnd.isAfter(to)) windowEnd = to;
                log.info("fetching verbatims window {}..{}", cursor, windowEnd);
                List<VerbatimDto> verbatims = client.fetchVerbatims(cursor, windowEnd);
                for (VerbatimDto verbatim : verbatims) {
                    List<SpeechMapper.FlatSpeech> speeches = mapper.flatten(verbatim);
                    seenSpeeches += speeches.size();
                    try {
                        tx.executeWithoutResult(status -> replaceSitting(verbatim.link(), speeches, roster));
                        upserted += speeches.size();
                    } catch (CallNotPermittedException e) {
                        throw e; // circuit breaker open → abort the run cleanly (FAILED)
                    } catch (Exception e) {
                        failedSittings++;
                        log.warn("failed verbatim sitting {} ({}): {}",
                                verbatim.link(), verbatim.title(), e.toString());
                    }
                }
                cursor = windowEnd.plusDays(1);
            }
            if (failedSittings > 0) {
                run.setStatus("PARTIAL");
                run.setErrorMessage(failedSittings + " sitting(s) failed and were skipped");
            } else {
                run.setStatus("SUCCESS");
            }
        } catch (Exception e) {
            log.error("speeches window import failed", e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
        } finally {
            run.setRecordsSeen(seenSpeeches);
            run.setRecordsUpserted(upserted);
            run.setFinishedAt(Instant.now());
            runLogRepo.save(run);
        }
        return run;
    }

    private record Roster(Map<String, PlenaryMember> byExternalId,
                          Map<String, List<PlenaryMember>> byFullName) {}

    private Roster loadRoster() {
        Map<String, PlenaryMember> byId = new HashMap<>();
        Map<String, List<PlenaryMember>> byName = new HashMap<>();
        for (PlenaryMember m : memberRepo.findAll()) {
            byId.put(m.getExternalId(), m);
            byName.computeIfAbsent(m.getFullName(), k -> new java.util.ArrayList<>()).add(m);
        }
        return new Roster(byId, byName);
    }

    /**
     * The verbatim speaker uuid is the Riigikogu person uuid, i.e. exactly our members'
     * external_id — so uuid match is primary and exact. Name matching (role prefix
     * stripping) only kicks in when the source omits the uuid.
     */
    private PlenaryMember resolveSpeaker(SpeechMapper.FlatSpeech f, Roster roster) {
        if (f.speakerUuid() != null) {
            return roster.byExternalId().get(f.speakerUuid());
        }
        return SpeechMapper.matchSpeaker(f.speakerRaw(), roster.byFullName()).orElse(null);
    }

    private void replaceSitting(String sittingLink, List<SpeechMapper.FlatSpeech> speeches,
                                Roster roster) {
        speechRepo.deleteBySourceNameAndSourceUrl(client.sourceName(), sittingLink);
        for (SpeechMapper.FlatSpeech f : speeches) {
            speechRepo.save(Speech.builder()
                    .sourceName(client.sourceName())
                    .speakerUuid(f.speakerUuid())
                    .plenaryMember(resolveSpeaker(f, roster))
                    .speakerRaw(f.speakerRaw())
                    .spokenAt(f.spokenAt())
                    .sittingTitle(f.sittingTitle())
                    .agendaItemTitle(f.agendaItemTitle())
                    .text(f.text())
                    .sourceUrl(f.sittingLink())
                    .importedAt(Instant.now())
                    .build());
        }
    }
}
