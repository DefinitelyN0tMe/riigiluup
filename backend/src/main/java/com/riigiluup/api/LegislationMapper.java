package com.riigiluup.api;

import com.riigiluup.group.GroupRepository;
import com.riigiluup.legislation.LegislativeItem;
import com.riigiluup.legislation.LegislativeItemTopic;
import com.riigiluup.legislation.LegislativeSponsorship;
import com.riigiluup.legislation.LegislativeStage;
import com.riigiluup.legislation.SponsorKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LegislationMapper {

    private final GroupRepository groupRepo;

    /**
     * The externalId to link a sponsor's committee page to — kept only for COMMITTEE sponsors whose
     * committee actually exists as a group (active or dissolved). Otherwise null, so the frontend
     * renders plain text instead of a dead /committees/{id} link (a handful of legacy sponsors
     * reference committee ids that no group row matches).
     */
    private String committeeLinkId(LegislativeSponsorship sp) {
        String extId = sp.getExternalId();
        if (sp.getSponsorKind() == SponsorKind.COMMITTEE && extId != null
                && groupRepo.findFirstByExternalId(extId).isEmpty()) {
            return null;
        }
        return extId;
    }

    public LegislationListItemDto toListItem(LegislativeItem i) {
        return new LegislationListItemDto(
                i.getId(),
                i.getExternalId(),
                i.getMark(),
                i.getDraftTypeCode(),
                i.getTitle(),
                i.getPhase() == null ? "OTHER" : i.getPhase().name(),
                i.getActiveStageSourceCode(),
                i.getInitiatedDate(),
                i.getAcceptedDate(),
                i.getLeadingCommitteeName(),
                sourceUrl(i.getExternalId())
        );
    }

    public LegislationDetailDto toDetail(
            LegislativeItem i,
            List<LegislativeStage> stages,
            List<LegislativeSponsorship> sponsors,
            List<LegislativeItemTopic> topics,
            List<com.riigiluup.vote.VoteEvent> votes,
            List<com.riigiluup.legislation.BillAmendment> amendments
    ) {
        return new LegislationDetailDto(
                i.getId(),
                i.getExternalId(),
                i.getMark(),
                i.getMembership(),
                i.getDraftTypeCode(),
                i.getTitle(),
                i.getInitialTitle(),
                i.getPhase() == null ? "OTHER" : i.getPhase().name(),
                i.getActiveStageSourceCode(),
                i.getActiveStatusSourceCode(),
                i.getProceedingStatus(),
                i.getActiveStatusDate(),
                i.getInitiatedDate(),
                i.getAcceptedDate(),
                i.getAmendmentsDeadline(),
                i.getIntroduction(),
                i.getLeadingCommitteeName(),
                stages.stream().map(s -> new LegislationDetailDto.StageDto(
                        s.getReadingCode(), s.getStatusCode(),
                        s.getOccurredAt(), s.getSequence())).toList(),
                sponsors.stream().map(sp -> new LegislationDetailDto.SponsorDto(
                        sp.getSponsorKind() == null ? "OTHER" : sp.getSponsorKind().name(),
                        sp.getDisplayName(),
                        sp.getPlenaryMember() == null ? null : sp.getPlenaryMember().getSlug(),
                        sp.getPlenaryMember() == null ? null : sp.getPlenaryMember().getFullName(),
                        committeeLinkId(sp))).toList(),
                topics.stream().map(it -> new LegislationDetailDto.TopicDto(
                        it.getTopic().getEdid(), it.getTopic().getText())).toList(),
                sourceUrl(i.getExternalId()),
                riigikoguPageUrl(i.getExternalId()),
                i.getRtActId(),
                i.getRtPublished(),
                votes.stream().map(v -> new LegislationDetailDto.BillVoteDto(
                        v.getId(), v.getVotingNumber(),
                        v.getType() == null ? null : v.getType().name(),
                        v.getDescription(), v.getStartedAt(),
                        // Real abstentions (neutral); resultAbstained is an overlapping source
                        // total (did-not-vote + absent) — see frontend lib/voteTally.
                        v.getResultInFavor(), v.getResultAgainst(), v.getResultNeutral())).toList(),
                amendments.stream().map(a -> new LegislationDetailDto.AmendmentDto(
                        a.getExternalId(),
                        a.getTitle(),
                        a.getReference(),
                        a.getFileUuid() == null ? null : fileDownloadUrl(a.getFileUuid()),
                        a.getFileName())).toList()
        );
    }

    /** Public Riigikogu file download for an amendment document (docx/pdf). */
    private static String fileDownloadUrl(String fileUuid) {
        return "https://api.riigikogu.ee/api/files/" + fileUuid + "/download";
    }

    private static String sourceUrl(String uuid) {
        return "https://api.riigikogu.ee/api/volumes/drafts/" + uuid;
    }

    /** Human-readable Riigikogu bill (eelnõu) page (kept alongside the raw API URL). */
    private static String riigikoguPageUrl(String uuid) {
        return "https://www.riigikogu.ee/tegevus/eelnoud/eelnou/" + uuid + "/";
    }
}
