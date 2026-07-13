package com.politico.legislation;

import com.politico.person.PlenaryMember;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "legislative_sponsorship")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegislativeSponsorship {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legislative_item_id")
    private LegislativeItem legislativeItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "sponsor_kind", nullable = false, length = 32)
    private SponsorKind sponsorKind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plenary_member_id")
    private PlenaryMember plenaryMember;

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Column(name = "display_name", length = 512)
    private String displayName;
}
