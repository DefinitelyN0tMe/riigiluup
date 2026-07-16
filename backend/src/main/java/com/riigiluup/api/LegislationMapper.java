package com.riigiluup.api;

import com.riigiluup.legislation.LegislativeItem;
import com.riigiluup.legislation.LegislativeItemTopic;
import com.riigiluup.legislation.LegislativeSponsorship;
import com.riigiluup.legislation.LegislativeStage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LegislationMapper {

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
            List<LegislativeItemTopic> topics
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
                        sp.getExternalId())).toList(),
                topics.stream().map(it -> new LegislationDetailDto.TopicDto(
                        it.getTopic().getEdid(), it.getTopic().getText())).toList(),
                sourceUrl(i.getExternalId())
        );
    }

    private static String sourceUrl(String uuid) {
        return "https://api.riigikogu.ee/api/volumes/drafts/" + uuid;
    }
}
