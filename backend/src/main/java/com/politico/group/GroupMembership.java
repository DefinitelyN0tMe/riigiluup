package com.politico.group;

import com.politico.person.PlenaryMember;
import com.politico.source.SourceSnapshot;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "group_membership")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupMembership {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plenary_member_id")
    private PlenaryMember plenaryMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MembershipRole role;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_snapshot_id")
    private SourceSnapshot sourceSnapshot;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
