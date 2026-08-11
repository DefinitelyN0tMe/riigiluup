package com.riigiluup.person;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One faction (parliamentary group) membership period of a sitting MP, from the Riigikogu detail
 * API. {@code memberExternalId} is the Riigikogu person id. Distinct from {@link MpPartyMembership}
 * (Wikidata party affiliation): a faction is the in-parliament group, and this is what changes when
 * an MP leaves a faction to sit as unaffiliated. Shown as a timeline on the profile.
 */
@Entity
@Table(name = "mp_faction_membership")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MpFactionMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_external_id", nullable = false, length = 64)
    private String memberExternalId;

    @Column(name = "faction_external_id", nullable = false, length = 64)
    private String factionExternalId;

    @Column(name = "faction_name", nullable = false, length = 256)
    private String factionName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
