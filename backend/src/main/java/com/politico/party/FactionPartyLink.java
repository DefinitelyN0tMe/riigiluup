package com.politico.party;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "faction_party_link")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FactionPartyLink {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "faction_external_id", nullable = false, length = 64)
    private String factionExternalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;
}
