package com.politico.ingestion.riigikogu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.politico.group.Group;
import com.politico.group.GroupMembership;
import com.politico.group.GroupMembershipRepository;
import com.politico.group.GroupRepository;
import com.politico.group.MembershipRole;
import com.politico.person.PlenaryMember;
import com.politico.person.PlenaryMemberRepository;
import com.politico.source.ProcessingStatus;
import com.politico.source.SourceSnapshot;
import com.politico.source.SourceSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class PlenaryMemberDetailImporter {

    private static final String JOB_NAME = "plenary-members.detail-refresh";
    private static final String ENTITY = "plenary-member-detail";

    private final RiigikoguClient client;
    private final PlenaryMemberDetailMapper mapper;
    private final ObjectMapper json;
    private final PlenaryMemberRepository memberRepo;
    private final GroupRepository groupRepo;
    private final GroupMembershipRepository membershipRepo;
    private final SourceSnapshotRepository snapshotRepo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;

    public PlenaryMemberDetailImporter(
            RiigikoguClient client,
            PlenaryMemberDetailMapper mapper,
            ObjectMapper json,
            PlenaryMemberRepository memberRepo,
            GroupRepository groupRepo,
            GroupMembershipRepository membershipRepo,
            SourceSnapshotRepository snapshotRepo,
            ImportRunLogRepository runLogRepo,
            org.springframework.transaction.PlatformTransactionManager txManager
    ) {
        this.client = client;
        this.mapper = mapper;
        this.json = json;
        this.memberRepo = memberRepo;
        this.groupRepo = groupRepo;
        this.membershipRepo = membershipRepo;
        this.snapshotRepo = snapshotRepo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * Iterate active plenary members and refresh detail + committee memberships.
     * Uses TransactionTemplate to get a fresh tx per MP (self-invoked
     * {@code @Transactional} would bypass the Spring proxy).
     */
    public ImportRunLog runOnce() {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(client.sourceName())
                .jobName(JOB_NAME)
                .startedAt(Instant.now())
                .status("RUNNING")
                .build());
        int seen = 0;
        int upserted = 0;
        try {
            int page = 0;
            Slice<PlenaryMember> slice;
            do {
                slice = memberRepo.findActiveOrderByLastName(PageRequest.of(page, 200));
                for (PlenaryMember m : slice.getContent()) {
                    seen++;
                    try {
                        tx.executeWithoutResult(status -> upsertOne(m.getExternalId()));
                        upserted++;
                    } catch (Exception e) {
                        log.warn("detail refresh failed for {} {}: {}",
                                m.getFullName(), m.getExternalId(), e.getMessage());
                    }
                }
                page++;
            } while (slice.hasNext());
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("detail refresh outer loop failed", e);
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

    private void upsertOne(String externalId) {
        PlenaryMemberDetailDto dto = client.fetchPlenaryMemberDetail(externalId);
        String payload;
        try { payload = json.writeValueAsString(dto); }
        catch (Exception e) { throw new IllegalStateException(e); }
        String hash = sha256(payload);

        SourceSnapshot snap = snapshotRepo
                .findFirstBySourceNameAndEntityTypeAndExternalIdAndPayloadHash(
                        client.sourceName(), ENTITY, dto.uuid(), hash)
                .orElseGet(() -> snapshotRepo.save(SourceSnapshot.builder()
                        .sourceName(client.sourceName())
                        .entityType(ENTITY)
                        .externalId(dto.uuid())
                        .payload(json.valueToTree(dto))
                        .payloadHash(hash)
                        .sourceUrl("https://api.riigikogu.ee/api/plenary-members/" + dto.uuid())
                        .fetchedAt(Instant.now())
                        .processingStatus(ProcessingStatus.PENDING)
                        .build()));

        PlenaryMember member = memberRepo
                .findBySourceNameAndExternalId(client.sourceName(), dto.uuid())
                .orElseThrow(() -> new IllegalStateException(
                        "member not found for external id " + dto.uuid()));

        mapper.applyDetail(member, dto);
        member.setSourceSnapshot(snap);
        memberRepo.save(member);

        reconcileCommitteeMemberships(member, dto, snap);
        snap.setProcessingStatus(ProcessingStatus.PROCESSED);
    }

    private void reconcileCommitteeMemberships(
            PlenaryMember member, PlenaryMemberDetailDto dto, SourceSnapshot snap
    ) {
        List<PlenaryMemberDetailDto.GroupRef> committees = mapper.currentTermCommittees(dto);
        Set<String> incomingGroupUuids = new HashSet<>();
        for (PlenaryMemberDetailDto.GroupRef c : committees) {
            if (c.uuid() == null) continue;
            incomingGroupUuids.add(c.uuid());
            Group group = groupRepo
                    .findBySourceNameAndExternalId(client.sourceName(), c.uuid())
                    .orElse(null);
            if (group == null) {
                log.debug("committee {} unknown, skipping membership — run usergroups importer first",
                        c.uuid());
                continue;
            }
            GroupMembership incoming = membershipRepo
                    .findByPlenaryMemberAndGroupAndStartDate(member, group, null)
                    .orElseGet(() -> GroupMembership.builder()
                            .plenaryMember(member)
                            .group(group)
                            .active(true)
                            .importedAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build());
            String roleLabel = c.membership() == null || c.membership().role() == null
                    ? null : c.membership().role().value();
            incoming.setRole(MembershipRole.fromSourceLabel(roleLabel));
            incoming.setActive(true);
            incoming.setSourceSnapshot(snap);
            incoming.setUpdatedAt(Instant.now());
            membershipRepo.save(incoming);
        }
        // Deactivate memberships not in the current detail response.
        for (GroupMembership existing : membershipRepo.findByPlenaryMemberAndActiveTrue(member)) {
            if (existing.getGroup() == null) continue;
            String gid = existing.getGroup().getExternalId();
            if (!incomingGroupUuids.contains(gid)) {
                existing.setActive(false);
                existing.setUpdatedAt(Instant.now());
                membershipRepo.save(existing);
            }
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
