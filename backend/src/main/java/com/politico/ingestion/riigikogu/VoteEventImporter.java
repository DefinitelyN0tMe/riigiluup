package com.politico.ingestion.riigikogu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.politico.person.PlenaryMember;
import com.politico.person.PlenaryMemberRepository;
import com.politico.source.ProcessingStatus;
import com.politico.source.SourceSnapshot;
import com.politico.source.SourceSnapshotRepository;
import com.politico.vote.IndividualVote;
import com.politico.vote.IndividualVoteRepository;
import com.politico.vote.VoteEvent;
import com.politico.vote.VoteEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VoteEventImporter {

    private static final String JOB_NAME = "votes.window-refresh";
    private static final String ENTITY_SUMMARY = "voting-summary";
    private static final String ENTITY_DETAIL = "voting-detail";

    private final RiigikoguClient client;
    private final VoteEventMapper eventMapper;
    private final IndividualVoteMapper voteMapper;
    private final ObjectMapper json;
    private final VoteEventRepository voteEventRepo;
    private final IndividualVoteRepository individualVoteRepo;
    private final PlenaryMemberRepository memberRepo;
    private final SourceSnapshotRepository snapshotRepo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;

    public VoteEventImporter(
            RiigikoguClient client,
            VoteEventMapper eventMapper,
            IndividualVoteMapper voteMapper,
            ObjectMapper json,
            VoteEventRepository voteEventRepo,
            IndividualVoteRepository individualVoteRepo,
            PlenaryMemberRepository memberRepo,
            SourceSnapshotRepository snapshotRepo,
            ImportRunLogRepository runLogRepo,
            PlatformTransactionManager txManager
    ) {
        this.client = client;
        this.eventMapper = eventMapper;
        this.voteMapper = voteMapper;
        this.json = json;
        this.voteEventRepo = voteEventRepo;
        this.individualVoteRepo = individualVoteRepo;
        this.memberRepo = memberRepo;
        this.snapshotRepo = snapshotRepo;
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
        int seenSittings = 0;
        int seenVotings = 0;
        int upsertedVotings = 0;
        try {
            LocalDate cursor = from;
            while (!cursor.isAfter(to)) {
                LocalDate windowEnd = cursor.plusDays(6);
                if (windowEnd.isAfter(to)) windowEnd = to;
                log.info("fetching votings window {}..{}", cursor, windowEnd);
                List<VotingListDto> sittings = client.fetchVotingsInWindow(cursor, windowEnd);
                client.throttle();
                seenSittings += sittings.size();
                for (VotingListDto sitting : sittings) {
                    if (sitting.votings() == null) continue;
                    for (VotingListDto.VotingSummary s : sitting.votings()) {
                        seenVotings++;
                        try {
                            tx.executeWithoutResult(status -> upsertVote(sitting, s));
                            upsertedVotings++;
                        } catch (Exception e) {
                            log.warn("failed vote {} ({}): {}",
                                    s.uuid(), s.description(), e.toString());
                        }
                        client.throttle();
                    }
                }
                cursor = windowEnd.plusDays(1);
            }
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("votes window import failed", e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
        } finally {
            run.setRecordsSeen(seenSittings + seenVotings);
            run.setRecordsUpserted(upsertedVotings);
            run.setFinishedAt(Instant.now());
            runLogRepo.save(run);
        }
        return run;
    }

    private void upsertVote(VotingListDto sitting, VotingListDto.VotingSummary summary) {
        VoteEvent existing = voteEventRepo
                .findBySourceNameAndExternalId(client.sourceName(), summary.uuid())
                .orElse(null);

        SourceSnapshot summarySnap = snapshotForSummary(sitting, summary);
        if (existing == null) {
            existing = eventMapper.toEntity(summary, sitting);
            existing.setSourceSnapshot(summarySnap);
            existing = voteEventRepo.save(existing);
        } else {
            eventMapper.applyDetail(existing, dummyDetail(summary, sitting));
            existing.setSourceSnapshot(summarySnap);
        }

        VotingDetailDto detail;
        try {
            detail = client.fetchVotingDetail(summary.uuid());
        } catch (Exception e) {
            log.warn("detail fetch failed for {}: {}", summary.uuid(), e.toString());
            summarySnap.setProcessingStatus(ProcessingStatus.PROCESSED);
            return;
        }
        SourceSnapshot detailSnap = snapshotForDetail(detail);
        eventMapper.applyDetail(existing, detail);
        existing.setSourceSnapshot(detailSnap);

        reconcileVoters(existing, detail);
        detailSnap.setProcessingStatus(ProcessingStatus.PROCESSED);
    }

    private static VotingDetailDto dummyDetail(
            VotingListDto.VotingSummary s, VotingListDto sitting) {
        return new VotingDetailDto(
                s.uuid(), s.votingNumber(), s.type(), s.description(),
                s.startDateTime(), s.endDateTime(),
                s.present(), s.absent(),
                s.inFavor(), s.against(), s.neutral(), s.abstained(),
                sitting == null ? null : new VotingDetailDto.Sitting(sitting.uuid(), sitting.title()),
                List.of()
        );
    }

    private void reconcileVoters(VoteEvent event, VotingDetailDto detail) {
        if (detail.voters() == null || detail.voters().isEmpty()) return;
        Map<String, PlenaryMember> byExternalId = memberRepo.findAll().stream()
                .collect(Collectors.toMap(PlenaryMember::getExternalId, Function.identity(),
                        (a, b) -> a));
        for (VotingDetailDto.Voter voter : detail.voters()) {
            PlenaryMember m = byExternalId.get(voter.uuid());
            if (m == null) {
                log.debug("voter {} not in plenary_member table — skipping", voter.uuid());
                continue;
            }
            IndividualVote incoming = individualVoteRepo
                    .findByVoteEventAndPlenaryMember(event, m)
                    .orElse(null);
            IndividualVote fresh = voteMapper.toEntity(event, m, voter);
            if (incoming == null) {
                individualVoteRepo.save(fresh);
            } else {
                incoming.setFactionExternalId(fresh.getFactionExternalId());
                incoming.setFactionName(fresh.getFactionName());
                incoming.setChoice(fresh.getChoice());
                incoming.setChoiceSourceCode(fresh.getChoiceSourceCode());
                individualVoteRepo.save(incoming);
            }
        }
    }

    private SourceSnapshot snapshotForSummary(VotingListDto sitting, VotingListDto.VotingSummary s) {
        return snapshotFor(ENTITY_SUMMARY, s.uuid(),
                new Object[]{sitting == null ? null : sitting.uuid(), s});
    }

    private SourceSnapshot snapshotForDetail(VotingDetailDto d) {
        return snapshotFor(ENTITY_DETAIL, d.uuid(), d);
    }

    private SourceSnapshot snapshotFor(String entity, String externalId, Object payload) {
        String payloadStr;
        try { payloadStr = json.writeValueAsString(payload); }
        catch (Exception e) { throw new IllegalStateException(e); }
        String hash = sha256(payloadStr);
        return snapshotRepo
                .findFirstBySourceNameAndEntityTypeAndExternalIdAndPayloadHash(
                        client.sourceName(), entity, externalId, hash)
                .orElseGet(() -> snapshotRepo.save(SourceSnapshot.builder()
                        .sourceName(client.sourceName())
                        .entityType(entity)
                        .externalId(externalId)
                        .payload(json.valueToTree(payload))
                        .payloadHash(hash)
                        .sourceUrl("https://api.riigikogu.ee/api/votings/" + externalId)
                        .fetchedAt(Instant.now())
                        .processingStatus(ProcessingStatus.PENDING)
                        .build()));
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
