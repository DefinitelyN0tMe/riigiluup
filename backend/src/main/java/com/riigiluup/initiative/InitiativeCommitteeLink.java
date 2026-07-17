package com.riigiluup.initiative;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * One initiative ↔ one committee. The source can assign several committees to an initiative,
 * packing them newline-separated into a single CSV field.
 *
 * <p>{@code committeeSlug} is always the source's raw value; {@code groupId} may be NULL when
 * the slug does not resolve to an active standing committee (renamed or newly added). That is
 * graceful degradation, not an import error — the UI then shows the slug without a link.
 */
@Entity
@Table(name = "initiative_committee")
@IdClass(InitiativeCommitteeLink.Key.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InitiativeCommitteeLink {

    @Id
    @Column(name = "initiative_id")
    private Long initiativeId;

    @Id
    @Column(name = "committee_slug", length = 32)
    private String committeeSlug;

    @Column(name = "group_id")
    private UUID groupId;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class Key implements Serializable {
        private Long initiativeId;
        private String committeeSlug;
    }
}
