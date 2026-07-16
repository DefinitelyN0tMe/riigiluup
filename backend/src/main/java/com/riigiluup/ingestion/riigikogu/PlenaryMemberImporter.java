package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.source.ProcessingStatus;
import com.riigiluup.source.SourceSnapshot;
import com.riigiluup.source.SourceSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlenaryMemberImporter {

    private static final String JOB_NAME = "plenary-members.full-refresh";

    private final RiigikoguClient client;
    private final PlenaryMemberMapper mapper;
    private final ObjectMapper json;
    private final PlenaryMemberRepository memberRepo;
    private final SourceSnapshotRepository snapshotRepo;
    private final ImportRunLogRepository runLogRepo;

    @Transactional
    public ImportRunLog runOnce() {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(client.sourceName())
                .jobName(JOB_NAME)
                .startedAt(Instant.now())
                .status("RUNNING")
                .build());

        try {
            List<PlenaryMemberDto> dtos = client.fetchAllPlenaryMembers();
            int upserted = 0;
            for (PlenaryMemberDto dto : dtos) {
                if (upsert(dto)) upserted++;
            }
            run.setRecordsSeen(dtos.size());
            run.setRecordsUpserted(upserted);
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("plenary-members import failed", e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
        } finally {
            run.setFinishedAt(Instant.now());
            runLogRepo.save(run);
        }
        return run;
    }

    private boolean upsert(PlenaryMemberDto dto) {
        String payloadStr;
        try {
            payloadStr = json.writeValueAsString(dto);
        } catch (Exception e) {
            throw new IllegalStateException("cannot serialize dto", e);
        }
        String hash = sha256(payloadStr);

        SourceSnapshot snap = snapshotRepo
                .findFirstBySourceNameAndEntityTypeAndExternalIdAndPayloadHash(
                        client.sourceName(), "plenary-member", dto.uuid(), hash)
                .orElseGet(() -> snapshotRepo.save(SourceSnapshot.builder()
                        .sourceName(client.sourceName())
                        .entityType("plenary-member")
                        .externalId(dto.uuid())
                        .payload(json.valueToTree(dto))
                        .payloadHash(hash)
                        .sourceUrl(
                                "https://api.riigikogu.ee/api/plenary-members/" + dto.uuid())
                        .fetchedAt(Instant.now())
                        .processingStatus(ProcessingStatus.PENDING)
                        .build()));

        PlenaryMember incoming = mapper.toEntity(dto);
        PlenaryMember existing = memberRepo
                .findBySourceNameAndExternalId(client.sourceName(), dto.uuid())
                .orElse(null);

        if (existing == null) {
            incoming.setSourceSnapshot(snap);
            memberRepo.save(incoming);
        } else {
            existing.setFirstName(incoming.getFirstName());
            existing.setLastName(incoming.getLastName());
            existing.setFullName(incoming.getFullName());
            existing.setOfficialProfileUrl(incoming.getOfficialProfileUrl());
            existing.setActive(incoming.isActive());
            // Photo and faction come from the richer detail endpoint (PlenaryMemberDetailImporter).
            // The /api/plenary-members list omits them for some members, so only refresh when the
            // list actually carries a value — never overwrite detail-populated data with a list null
            // (that stranded MPs with a blank photo + "no faction" whenever a later detail refresh
            // failed, e.g. on a 429 mid-run).
            if (incoming.getPhotoUrl() != null) {
                existing.setPhotoUrl(incoming.getPhotoUrl());
            }
            if (incoming.getFactionExternalId() != null) {
                existing.setFactionExternalId(incoming.getFactionExternalId());
                existing.setFactionName(incoming.getFactionName());
            }
            existing.setSourceSnapshot(snap);
            existing.setUpdatedAt(Instant.now());
        }
        snap.setProcessingStatus(ProcessingStatus.PROCESSED);
        return true;
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
