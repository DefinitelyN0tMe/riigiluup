package com.riigiluup.legislation;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legislative_stage")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegislativeStage {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legislative_item_id")
    private LegislativeItem legislativeItem;

    @Column(name = "reading_code", length = 64)
    private String readingCode;

    @Column(name = "status_code", length = 64)
    private String statusCode;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(nullable = false)
    private int sequence;
}
