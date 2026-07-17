package com.riigiluup.person;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One party-membership period of a sitting MP, from Wikidata P102 (start/end qualifiers).
 * {@code memberExternalId} is the Riigikogu person id, matched by the WikidataImporter.
 * Distinct from faction and from the party an MP ran for — shown separately on the profile.
 */
@Entity
@Table(name = "mp_party_membership")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MpPartyMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_external_id", nullable = false, length = 64)
    private String memberExternalId;

    @Column(name = "party_qid", nullable = false, length = 32)
    private String partyQid;

    @Column(name = "party_label", nullable = false, length = 256)
    private String partyLabel;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
