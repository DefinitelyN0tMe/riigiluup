package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riigiluup.group.Group;
import com.riigiluup.group.GroupMembership;
import com.riigiluup.group.GroupMembershipRepository;
import com.riigiluup.group.GroupRepository;
import com.riigiluup.group.MembershipRole;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.source.ProcessingStatus;
import com.riigiluup.source.SourceSnapshot;
import com.riigiluup.source.SourceSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final com.riigiluup.person.MpFactionMembershipRepository factionHistoryRepo;
    private final TransactionTemplate tx;

    /** Re-fetch a member's detail at most this often — committees/bio/faction change slowly. */
    @Value("${riigiluup.schedule.member-detail-max-age-days:7}")
    private int detailMaxAgeDays;

    public PlenaryMemberDetailImporter(
            RiigikoguClient client,
            PlenaryMemberDetailMapper mapper,
            ObjectMapper json,
            PlenaryMemberRepository memberRepo,
            GroupRepository groupRepo,
            GroupMembershipRepository membershipRepo,
            SourceSnapshotRepository snapshotRepo,
            ImportRunLogRepository runLogRepo,
            com.riigiluup.person.MpFactionMembershipRepository factionHistoryRepo,
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
        this.factionHistoryRepo = factionHistoryRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * Iterate active plenary members and refresh detail + committee memberships.
     * Uses TransactionTemplate to get a fresh tx per MP (self-invoked
     * {@code @Transactional} would bypass the Spring proxy).
     */
    public ImportRunLog runOnce() {
        return runOnce(false);
    }

    /**
     * @param force re-fetch every active member even if their detail is still within the freshness
     *              window. Used to pick up a change that just happened (e.g. a faction departure)
     *              and to backfill the faction-history table, without waiting for the window.
     */
    public ImportRunLog runOnce(boolean force) {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(client.sourceName())
                .jobName(JOB_NAME)
                .startedAt(Instant.now())
                .status("RUNNING")
                .build());
        int seen = 0;
        int upserted = 0;
        int skipped = 0;
        Instant freshCutoff = Instant.now().minus(detailMaxAgeDays, ChronoUnit.DAYS);
        try {
            int page = 0;
            Slice<PlenaryMember> slice;
            do {
                slice = memberRepo.findActiveOrderByLastName(PageRequest.of(page, 200));
                for (PlenaryMember m : slice.getContent()) {
                    seen++;
                    // Skip members whose detail we already refreshed within the window — re-fetching
                    // all 101 every run just re-downloads unchanged bio/committees/faction. New
                    // members (no snapshot) and stale ones are still fetched, so none stay incomplete.
                    // `force` bypasses this to catch a just-happened change or backfill history.
                    if (!force && snapshotRepo.existsBySourceNameAndEntityTypeAndExternalIdAndFetchedAtAfter(
                            client.sourceName(), ENTITY, m.getExternalId(), freshCutoff)) {
                        skipped++;
                        continue;
                    }
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
            log.info("detail refresh: {} fetched, {} skipped as fresh (< {} days)",
                    upserted, skipped, detailMaxAgeDays);
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

        writeFactionHistory(dto.uuid(), dto);
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

    /**
     * Replace a member's faction-membership timeline from the current parliamentary term's factions,
     * each carrying a start/end span. This is what lets the profile show when an MP joined or left a
     * faction, rather than only their current one.
     */
    private void writeFactionHistory(String memberExternalId, PlenaryMemberDetailDto dto) {
        factionHistoryRepo.deleteByMemberExternalId(memberExternalId);
        PlenaryMemberDetailDto.Membership term = PlenaryMemberDetailMapper.currentTerm(dto);
        if (term == null || term.factions() == null) return;
        Instant now = Instant.now();
        // Dedup on (faction, startDate) to honour the unique constraint, keeping the last seen.
        java.util.Map<String, com.riigiluup.person.MpFactionMembership> rows = new java.util.LinkedHashMap<>();
        for (PlenaryMemberDetailDto.GroupRef f : term.factions()) {
            if (f.uuid() == null || f.name() == null) continue;
            PlenaryMemberDetailDto.MembershipSpan span = f.membership();
            java.time.LocalDate start = parseDate(span == null ? null : span.startDate());
            java.time.LocalDate end = parseDate(span == null ? null : span.endDate());
            rows.put(f.uuid() + "|" + start, com.riigiluup.person.MpFactionMembership.builder()
                    .memberExternalId(memberExternalId)
                    .factionExternalId(f.uuid())
                    .factionName(f.name())
                    .startDate(start)
                    .endDate(end)
                    .importedAt(now)
                    .build());
        }
        factionHistoryRepo.saveAll(rows.values());
    }

    private static java.time.LocalDate parseDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return java.time.LocalDate.parse(iso); }
        catch (Exception e) { return null; }
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
