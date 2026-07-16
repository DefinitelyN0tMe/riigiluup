package com.riigiluup.finance;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A political party's declared income for one year and income type, from the ERJK
 * (Political Parties Financing Surveillance Committee) open data — the "money in
 * politics" source. One row per (party, category, year); refreshed by code.
 */
@Entity
@Table(name = "party_receipt")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartyReceipt {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "erjk_party_id", nullable = false)
    private int erjkPartyId;

    @Column(name = "party_name", nullable = false, length = 256)
    private String partyName;

    /** ERJK income-type id, e.g. 111 Rahaline annetus (monetary donation), 113 Riigitoetus (state support). */
    @Column(name = "category_id", nullable = false, length = 16)
    private String categoryId;

    @Column(name = "category_name", nullable = false, length = 128)
    private String categoryName;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
