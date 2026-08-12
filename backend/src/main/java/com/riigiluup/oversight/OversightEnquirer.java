package com.riigiluup.oversight;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * One MP who put an oversight question (a question can have several co-enquirers).
 * {@code memberExternalId} matches {@code plenary_member.external_id}. Flat by design (item id as a
 * plain column) so the importer can delete-and-reinsert a question's enquirers cheaply.
 */
@Entity
@Table(name = "oversight_enquirer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OversightEnquirer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oversight_item_id", nullable = false)
    private UUID oversightItemId;

    @Column(name = "member_external_id", nullable = false, length = 64)
    private String memberExternalId;

    @Column(name = "member_name", length = 256)
    private String memberName;
}
