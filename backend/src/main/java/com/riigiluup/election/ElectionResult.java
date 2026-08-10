package com.riigiluup.election;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One campaign of a current MP at a published election (RK / EP / KOV) — personal votes,
 * whether they were elected, mandate type (when elected), party and district. Sourced from
 * the State Electoral Office open data (opendata.valimised.ee, CC BY 4.0), matched to the MP
 * by name; only high-confidence (name unique on both sides) matches are stored.
 */
@Entity
@Table(name = "mp_election_result")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ElectionResult {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "member_external_id", nullable = false, length = 64)
    private String memberExternalId;

    /** e.g. "RK_2023", "EP_2024", "KOV_2021" (type_year). */
    @Column(name = "election_code", nullable = false, length = 32)
    private String electionCode;

    /** Whether the candidate won a seat at this election (false for a losing or substitute run). */
    @Column(name = "elected", nullable = false)
    private boolean elected;

    @Column(name = "personal_votes", nullable = false)
    private int personalVotes;

    /** PERSONAL | DISTRICT | COMPENSATION | SUBSTITUTE; null when not elected. */
    @Column(name = "mandate_type", length = 32)
    private String mandateType;

    @Column(name = "district_number")
    private Integer districtNumber;

    /** Named district (used for historical rows, whose district numbering is not stable). */
    @Column(name = "district_name", length = 256)
    private String districtName;

    /**
     * True for a pre-2023 Riigikogu candidacy from Martin Mölder's compiled historical dataset
     * (shown in a separate, attributed block); false for the open-data footprint (RK_2023/EP/KOV).
     */
    @Column(name = "historical", nullable = false)
    private boolean historical;

    /** Party the MP ran for at the election (may differ from their current faction). */
    @Column(name = "party_name", length = 256)
    private String partyName;

    /** Registration (ballot) number on the district list. */
    @Column(name = "ballot_number")
    private Integer ballotNumber;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
