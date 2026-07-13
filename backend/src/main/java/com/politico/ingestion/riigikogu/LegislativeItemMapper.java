package com.politico.ingestion.riigikogu;

import com.politico.legislation.LegislationPhase;
import com.politico.legislation.LegislativeItem;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class LegislativeItemMapper {

    private static final String SOURCE = "riigikogu";

    public LegislativeItem fromListEntry(DraftListDto.DraftListEntry e) {
        Instant now = Instant.now();
        return LegislativeItem.builder()
                .externalId(e.uuid())
                .sourceName(SOURCE)
                .mark(e.mark())
                .membership(e.membership())
                .title(e.title() == null ? "(no title)" : e.title())
                .draftTypeCode(e.draftTypeCode())
                .phase(LegislationPhase.fromStageCode(e.activeDraftStage()))
                .activeStageSourceCode(e.activeDraftStage())
                .activeStatusSourceCode(e.activeDraftStatus())
                .proceedingStatus(e.proceedingStatus())
                .activeStatusDate(parseDate(e.activeDraftStatusDate()))
                .initiatedDate(parseDate(e.initiated()))
                .amendmentsDeadline(parseTs(e.amendmentsDeadline()))
                .leadingCommitteeExternalId(e.leadingCommittee() == null ? null : e.leadingCommittee().uuid())
                .leadingCommitteeName(e.leadingCommittee() == null ? null : e.leadingCommittee().name())
                .importedAt(now)
                .updatedAt(now)
                .build();
    }

    public void applyDetail(LegislativeItem target, DraftDetailDto d) {
        target.setMark(d.mark());
        target.setMembership(d.membership());
        if (d.title() != null) target.setTitle(d.title());
        target.setInitialTitle(d.initialTitle());
        target.setDraftTypeCode(d.draftTypeCode());
        target.setPhase(LegislationPhase.fromStageCode(d.activeDraftStage()));
        target.setActiveStageSourceCode(d.activeDraftStage());
        target.setActiveStatusSourceCode(d.activeDraftStatus());
        target.setIntroduction(d.introduction());
        target.setInitiatedDate(parseDate(d.initiated()));
        target.setAcceptedDate(parseDate(d.accepted()));
        target.setAmendmentsDeadline(parseTs(d.amendmentsDeadline()));
        if (d.leadingCommittee() != null) {
            target.setLeadingCommitteeExternalId(d.leadingCommittee().uuid());
            target.setLeadingCommitteeName(d.leadingCommittee().name());
        }
        target.setUpdatedAt(Instant.now());
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); }
        catch (Exception e) { return null; }
    }

    private static Instant parseTs(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s).toInstant(ZoneOffset.UTC); }
        catch (Exception e) { return null; }
    }
}
