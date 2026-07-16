package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riigiluup.group.Group;
import com.riigiluup.group.GroupRepository;
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
public class UsergroupImporter {

    private static final String JOB_NAME = "usergroups.full-refresh";
    private static final String ENTITY = "usergroup";

    private final RiigikoguClient client;
    private final UsergroupMapper mapper;
    private final ObjectMapper json;
    private final GroupRepository groupRepo;
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
            List<UsergroupDto> dtos = client.fetchAllUsergroups();
            int upserted = 0;
            for (UsergroupDto dto : dtos) {
                if (upsert(dto)) upserted++;
            }
            run.setRecordsSeen(dtos.size());
            run.setRecordsUpserted(upserted);
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("usergroups import failed", e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
        } finally {
            run.setFinishedAt(Instant.now());
            runLogRepo.save(run);
        }
        return run;
    }

    private boolean upsert(UsergroupDto dto) {
        String payload;
        try {
            payload = json.writeValueAsString(dto);
        } catch (Exception e) {
            throw new IllegalStateException("cannot serialize usergroup", e);
        }
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
                        .sourceUrl("https://api.riigikogu.ee/api/usergroups/" + dto.uuid())
                        .fetchedAt(Instant.now())
                        .processingStatus(ProcessingStatus.PENDING)
                        .build()));

        Group incoming = mapper.toEntity(dto);
        Group existing = groupRepo
                .findBySourceNameAndExternalId(client.sourceName(), dto.uuid())
                .orElse(null);

        if (existing == null) {
            incoming.setSourceSnapshot(snap);
            groupRepo.save(incoming);
        } else {
            existing.setType(incoming.getType());
            existing.setName(incoming.getName());
            existing.setShortName(incoming.getShortName());
            existing.setColorHex(incoming.getColorHex());
            existing.setSecretariatName(incoming.getSecretariatName());
            existing.setActive(incoming.isActive());
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
