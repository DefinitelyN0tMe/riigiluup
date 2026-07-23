package com.riigiluup.vote;

import com.riigiluup.source.SourceSnapshot;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vote_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoteEvent {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    @Column(name = "voting_number")
    private Integer votingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VoteEventType type;

    @Column(name = "type_source_code", length = 64)
    private String typeSourceCode;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "sitting_external_id", length = 64)
    private String sittingExternalId;

    @Column(name = "sitting_title", length = 512)
    private String sittingTitle;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "result_in_favor", nullable = false)
    private int resultInFavor;

    @Column(name = "result_against", nullable = false)
    private int resultAgainst;

    @Column(name = "result_abstained", nullable = false)
    private int resultAbstained;

    @Column(name = "result_neutral", nullable = false)
    private int resultNeutral;

    @Column(name = "result_present", nullable = false)
    private int resultPresent;

    @Column(name = "result_absent", nullable = false)
    private int resultAbsent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_snapshot_id")
    private SourceSnapshot sourceSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legislative_item_id")
    // Batch lazy loads so the votes list (native richSearch can't @EntityGraph) resolves
    // bill titles for a whole page in one IN-query instead of N per-row selects.
    @org.hibernate.annotations.BatchSize(size = 200)
    private com.riigiluup.legislation.LegislativeItem legislativeItem;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
