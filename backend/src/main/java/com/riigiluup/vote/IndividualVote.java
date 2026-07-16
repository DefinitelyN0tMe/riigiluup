package com.riigiluup.vote;

import com.riigiluup.person.PlenaryMember;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "individual_vote")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IndividualVote {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_event_id")
    private VoteEvent voteEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plenary_member_id")
    private PlenaryMember plenaryMember;

    @Column(name = "faction_external_id", length = 64)
    private String factionExternalId;

    @Column(name = "faction_name", length = 256)
    private String factionName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VoteChoice choice;

    @Column(name = "choice_source_code", length = 64)
    private String choiceSourceCode;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
