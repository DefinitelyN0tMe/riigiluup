package com.riigiluup.legislation;

import com.riigiluup.source.SourceSnapshot;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "legislative_item")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegislativeItem {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    private Integer mark;
    private Integer membership;

    @Column(nullable = false, length = 1024)
    private String title;

    @Column(name = "initial_title", length = 1024)
    private String initialTitle;

    @Column(name = "draft_type_code", length = 16)
    private String draftTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LegislationPhase phase;

    @Column(name = "active_stage_source_code", length = 64)
    private String activeStageSourceCode;

    @Column(name = "active_status_source_code", length = 64)
    private String activeStatusSourceCode;

    @Column(name = "proceeding_status", length = 32)
    private String proceedingStatus;

    @Column(name = "active_status_date")
    private LocalDate activeStatusDate;

    @Column(columnDefinition = "text")
    private String introduction;

    @Column(name = "initiated_date")
    private LocalDate initiatedDate;

    @Column(name = "accepted_date")
    private LocalDate acceptedDate;

    /** Riigi Teataja act id — encodes the official citation; null until (unambiguously) linked. */
    @Column(name = "rt_act_id")
    private Long rtActId;

    @Column(name = "rt_published")
    private LocalDate rtPublished;

    @Column(name = "amendments_deadline")
    private Instant amendmentsDeadline;

    @Column(name = "leading_committee_external_id", length = 64)
    private String leadingCommitteeExternalId;

    @Column(name = "leading_committee_name", length = 256)
    private String leadingCommitteeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_snapshot_id")
    private SourceSnapshot sourceSnapshot;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
