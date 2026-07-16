package com.politico.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Last successful attendance (participation) figure fetched from the Riigikogu statistics API,
 * persisted per MP so the profile metric can fall back to it instead of showing "0 of 0" when the
 * live source is temporarily unavailable. One row per member (external id is the natural key).
 */
@Entity
@Table(name = "member_participation_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberParticipationCache {

    @Id
    @Column(name = "member_external_id", length = 64)
    private String memberExternalId;

    @Column(nullable = false)
    private int sittings;

    @Column(nullable = false)
    private int attended;

    @Column(nullable = false)
    private double rate;

    @Column(name = "period_from")
    private LocalDate periodFrom;

    @Column(name = "period_to")
    private LocalDate periodTo;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;
}
