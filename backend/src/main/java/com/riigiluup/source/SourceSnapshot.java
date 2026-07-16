package com.riigiluup.source;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_snapshot")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SourceSnapshot {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Type(JsonBinaryType.class)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    private ProcessingStatus processingStatus;

    @Column(name = "processing_error", columnDefinition = "text")
    private String processingError;
}
