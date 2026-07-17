package com.riigiluup.initiative;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A citizen initiative from rahvaalgatus.ee, with all 17 source columns kept verbatim.
 *
 * <p>{@code destination} != "parliament" marks a municipal initiative (Tallinn, Narva, rural
 * municipalities). Those are imported but never displayed or counted: their signature
 * threshold is 1% of residents, not the flat 1000 the law sets for Riigikogu.
 *
 * <p>{@code legislativeItemId} is curated by an admin — the source carries no bill reference.
 * NULL is the normal state.
 */
@Entity
@Table(name = "initiative")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Initiative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(length = 64)
    private String uuid;

    @Column(columnDefinition = "text")
    private String title;

    /** Free-text author name, usually a private citizen. Display only — never aggregate. */
    @Column(columnDefinition = "text")
    private String authors;

    @Column(length = 64)
    private String destination;

    @Convert(converter = InitiativePhaseConverter.class)
    @Column(length = 24)
    private InitiativePhase phase;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "signing_started_at")
    private Instant signingStartedAt;

    @Column(name = "signing_ends_at")
    private Instant signingEndsAt;

    @Column(name = "signature_count")
    private Integer signatureCount;

    @Column(name = "last_signed_at")
    private Instant lastSignedAt;

    @Column(name = "sent_to_parliament_at")
    private Instant sentToParliamentAt;

    @Convert(converter = ParliamentDecisionConverter.class)
    @Column(name = "parliament_decision", length = 32)
    private ParliamentDecision parliamentDecision;

    @Column(name = "finished_in_parliament_at")
    private Instant finishedInParliamentAt;

    @Column(name = "sent_to_government_at")
    private Instant sentToGovernmentAt;

    @Column(name = "finished_in_government_at")
    private Instant finishedInGovernmentAt;

    @Column(name = "legislative_item_id")
    private UUID legislativeItemId;

    @Column(name = "linked_by", length = 255)
    private String linkedBy;

    @Column(name = "linked_at")
    private Instant linkedAt;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
