package com.riigiluup.party;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "mp_external_affiliation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalAffiliation {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "member_slug", nullable = false, length = 256)
    private String memberSlug;

    @Column(nullable = false, length = 256)
    private String organization;

    @Column(name = "org_kind", nullable = false, length = 32)
    private String orgKind;

    @Column(length = 256)
    private String role;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "source_url", nullable = false, columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "source_label", nullable = false, length = 256)
    private String sourceLabel;

    @Column(name = "verified_by", nullable = false, length = 128)
    private String verifiedBy;

    @Column(name = "verified_at", nullable = false)
    private LocalDate verifiedAt;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
