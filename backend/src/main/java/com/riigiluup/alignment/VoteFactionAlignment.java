package com.riigiluup.alignment;

import com.riigiluup.vote.VoteChoice;
import com.riigiluup.vote.VoteEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vote_faction_alignment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoteFactionAlignment {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_event_id")
    private VoteEvent voteEvent;

    @Column(name = "faction_external_id", nullable = false, length = 64)
    private String factionExternalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "majority_choice", length = 32)
    private VoteChoice majorityChoice;

    @Column(name = "majority_count", nullable = false)
    private int majorityCount;

    @Column(name = "comparable_count", nullable = false)
    private int comparableCount;

    @Column(name = "has_clear_majority", nullable = false)
    private boolean hasClearMajority;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}
