package com.politico.ingestion.riigikogu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.politico.alignment.FactionAlignmentComputer;
import com.politico.alignment.VoteFactionAlignment;
import com.politico.alignment.VoteFactionAlignmentRepository;
import com.politico.person.PlenaryMember;
import com.politico.person.PlenaryMemberRepository;
import com.politico.source.ProcessingStatus;
import com.politico.source.SourceSnapshot;
import com.politico.source.SourceSnapshotRepository;
import com.politico.vote.IndividualVote;
import com.politico.vote.IndividualVoteRepository;
import com.politico.vote.VoteEvent;
import com.politico.vote.VoteEventRepository;
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
    private final PlenaryMemberMapper memberMapper;
    private final ObjectMapper json;
    private final VoteEventRepository voteEventRepo;
    private final IndividualVoteRepository individualVoteRepo;
    private final PlenaryMemberRepository memberRepo;
    private final SourceSnapshotRepository snapshotRepo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;
    private final FactionAlignmentComputer alignmentComputer;
    private final VoteFactionAlignmentRepository alignmentRepo;

    public VoteEventImporter(
            RiigikoguClient client,
            VoteEventMapper eventMapper,
            IndividualVoteMapper voteMapper,
            PlenaryMemberMapper memberMapper,
            ObjectMapper json,
            VoteEventRepository voteEventRepo,
            IndividualVoteRepository individualVoteRepo,
            PlenaryMemberRepository memberRepo,
            SourceSnapshotRepository snapshotRepo,
            ImportRunLogRepository runLogRepo,
            PlatformTransactionManager txManager,
            FactionAlignmentComputer alignmentComputer,
            VoteFactionAlignmentRepository alignmentRepo
    ) {
        this.client = client;
        this.eventMapper = eventMapper;
        this.voteMapper = voteMapper;
        this.memberMapper = memberMapper;
        this.json = json;
        this.voteEventRepo = voteEventRepo;
        this.individualVoteRepo = individualVoteRepo;
        this.memberRepo = memberRepo;
        this.snapshotRepo = snapshotRepo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
        this.alignmentComputer = alignmentComputer;
        this.alignmentRepo = alignmentRepo;
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
        int failedVotings = 0;
        try {
            // Snapshot the plenary_member table once per run: it has ~100 rows and
            // rarely changes mid-window, so reloading it for every vote (was: findAll()
            // inside reconcileVoters) turned each vote into an N+1 hot path. A member
            // that appears between the snapshot and reconcileVoters just gets skipped
            // this run and reconciled on the next scheduled window.
            Map<String, PlenaryMember> memberCache = memberRepo.findAll().stream()
                    .collect(Collectors.toMap(PlenaryMember::getExternalId, Function.identity(),
                            (a, b) -> a));
            LocalDate cursor = from;
            while (!cursor.isAfter(to)) {
                LocalDate windowEnd = cursor.plusDays(6);
                if (windowEnd.isAfter(to)) windowEnd = to;
                log.info("fetching votings window {}..{}", cursor, windowEnd);
                List<VotingListDto> sittings = client.fetchVotingsInWindow(cursor, windowEnd);
                seenSittings += sittings.size();
                for (VotingListDto sitting : sittings) {
                    if (sitting.votings() == null) continue;
                    for (VotingListDto.VotingSummary s : sitting.votings()) {
                        seenVotings++;
                        try {
                            tx.executeWithoutResult(status -> upsertVote(sitting, s, memberCache));
                            upsertedVotings++;
                        } catch (CallNotPermittedException e) {
                            throw e; // circuit breaker open → abort the run cleanly (FAILED), don't march on
                        } catch (Exception e) {
                            failedVotings++;
                            log.warn("failed vote {} ({}): {}",
                                    s.uuid(), s.description(), e.toString());
                        }
                    }
                }
                cursor = windowEnd.plusDays(1);
            }
            if (failedVotings > 0) {
                run.setStatus("PARTIAL");
                run.setErrorMessage(failedVotings + " voting(s) failed detail fetch and were skipped");
            } else {
                run.setStatus("SUCCESS");
            }
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

    private void upsertVote(VotingListDto sitting, VotingListDto.VotingSummary summary,
                            Map<String, PlenaryMember> memberCache) {
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
        } catch (CallNotPermittedException e) {
            throw e; // circuit-broken upstream → fail the run instead of persisting a voteless event
        } catch (Exception e) {
            // Roll back this event rather than store it with zero individual votes; the caller
            // counts it as a failed voting and the window is retried on the next run.
            throw new IllegalStateException("detail fetch failed for " + summary.uuid(), e);
        }
        SourceSnapshot detailSnap = snapshotForDetail(detail);
        eventMapper.applyDetail(existing, detail);
        existing.setSourceSnapshot(detailSnap);

        reconcileVoters(existing, detail, memberCache);
        recomputeAlignmentsForEvent(existing);
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

    private void reconcileVoters(VoteEvent event, VotingDetailDto detail,
                                 Map<String, PlenaryMember> byExternalId) {
        if (detail.voters() == null || detail.voters().isEmpty()) return;
        for (VotingDetailDto.Voter voter : detail.voters()) {
            PlenaryMember m = byExternalId.get(voter.uuid());
            if (m == null) {
                // Historical MP not in the current mandate roster — materialise a stub
                // from the voter payload itself so we don't silently drop the vote.
                m = materialiseHistoricalVoter(voter, byExternalId);
                if (m == null) continue;
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

    /**
     * Materialise a stub {@link PlenaryMember} for a voter that isn't in the current
     * mandate roster. Uses the {@code Voter} payload itself (uuid, fullName, faction)
     * so we don't pay an extra Riigikogu API request per historical MP. The stub is
     * saved with {@code active=false} so the frontend can render it as "endine saadik".
     *
     * <p>Slug collisions (two historical MPs with identical fullName) are resolved by
     * appending the first 8 characters of the UUID.
     */
    private PlenaryMember materialiseHistoricalVoter(
            VotingDetailDto.Voter voter, Map<String, PlenaryMember> cache) {
        // Re-check the DB before inserting — another concurrent tx (or a prior loop
        // iteration in the same run) may have already created this stub.
        PlenaryMember existing = memberRepo
                .findBySourceNameAndExternalId(client.sourceName(), voter.uuid())
                .orElse(null);
        if (existing != null) {
            cache.put(voter.uuid(), existing);
            return existing;
        }
        String factionExtId = voter.faction() == null ? null : voter.faction().uuid();
        String factionName = voter.faction() == null ? null : voter.faction().name();
        PlenaryMember stub = memberMapper.stubFromVoter(
                voter.uuid(), voter.fullName(), factionExtId, factionName);
        if (memberRepo.findBySlug(stub.getSlug()).isPresent()) {
            stub.setSlug(stub.getSlug() + "-" + voter.uuid().substring(0, 8));
        }
        try {
            PlenaryMember saved = memberRepo.save(stub);
            cache.put(voter.uuid(), saved);
            log.debug("materialised historical voter {} ({})", voter.uuid(), voter.fullName());
            return saved;
        } catch (Exception e) {
            log.warn("failed to materialise historical voter {} ({}): {}",
                    voter.uuid(), voter.fullName(), e.toString());
            return null;
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

    private void recomputeAlignmentsForEvent(VoteEvent event) {
        List<IndividualVote> all = individualVoteRepo
                .findByVoteEventOrderByFactionNameAscPlenaryMember_LastNameAsc(event);
        Map<String, List<IndividualVote>> byFaction = new java.util.LinkedHashMap<>();
        for (IndividualVote iv : all) {
            String key = iv.getFactionExternalId();
            if (key == null) continue;
            byFaction.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(iv);
        }
        // Clear existing rows for this event to keep the aggregate authoritative on rerun.
        alignmentRepo.deleteByVoteEvent(event);
        for (Map.Entry<String, List<IndividualVote>> e : byFaction.entrySet()) {
            FactionAlignmentComputer.Result r = alignmentComputer.compute(e.getValue());
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

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
