package com.riigiluup.group;

import com.riigiluup.source.SourceSnapshot;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"group\"")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Group {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GroupType type;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(name = "short_name", length = 64)
    private String shortName;

    @Column(name = "color_hex", length = 16)
    private String colorHex;

    @Column(name = "secretariat_name", length = 256)
    private String secretariatName;

    @Column(nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_snapshot_id")
    private SourceSnapshot sourceSnapshot;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
