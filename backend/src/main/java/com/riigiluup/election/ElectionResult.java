package com.riigiluup.election;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * How a currently seated MP won their seat at a Riigikogu election — personal vote
 * count, mandate type and electoral district. Sourced from the State Electoral
 * Office open data (opendata.valimised.ee, CC BY 4.0), matched to the MP by name.
 */
@Entity
@Table(name = "mp_election_result")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ElectionResult {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "member_external_id", nullable = false, length = 64)
    private String memberExternalId;

    /** e.g. "RK_2023" (Riigikogu election 2023). */
    @Column(name = "election_code", nullable = false, length = 32)
    private String electionCode;

    @Column(name = "personal_votes", nullable = false)
    private int personalVotes;

    /** PERSONAL | DISTRICT | COMPENSATION. */
    @Column(name = "mandate_type", nullable = false, length = 32)
    private String mandateType;

    @Column(name = "district_number")
    private Integer districtNumber;

    /** Party the MP ran for at the election (may differ from their current faction). */
    @Column(name = "party_name", length = 256)
    private String partyName;

    /** Registration (ballot) number on the district list. */
    @Column(name = "ballot_number")
    private Integer ballotNumber;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
