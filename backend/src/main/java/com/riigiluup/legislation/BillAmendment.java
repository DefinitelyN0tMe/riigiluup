package com.riigiluup.legislation;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One amendment proposal (muudatusettepanek) to a bill, from the Riigikogu draft detail API. The
 * {@code title} names the proposing faction/MP and the bill; {@code fileUuid}/{@code fileName} point
 * to the public amendment document. Full-replace per bill on each detail refresh, like
 * {@link LegislativeStage}.
 */
@Entity
@Table(name = "bill_amendment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillAmendment {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legislative_item_id")
    private LegislativeItem legislativeItem;

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "reference", length = 160)
    private String reference;

    @Column(name = "file_uuid", length = 64)
    private String fileUuid;

    @Column(name = "file_name", length = 512)
    private String fileName;

    @Column(nullable = false)
    private int sequence;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
