package com.riigiluup.speech;

import com.riigiluup.person.PlenaryMember;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One speech from a Riigikogu plenary verbatim record. No source_snapshot: the raw payload
 * is megabytes of text per sitting week; source_url points at the public stenogram instead.
 * The tsv search column is DB-generated (V23) and deliberately unmapped.
 */
@Entity
@Table(name = "speech")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Speech {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plenary_member_id")
    private PlenaryMember plenaryMember;

    /** Speaker exactly as printed in the verbatim, incl. role prefix ("Esimees Lauri Hussar"). */
    @Column(name = "speaker_raw", nullable = false, length = 255)
    private String speakerRaw;

    @Column(name = "spoken_at", nullable = false)
    private Instant spokenAt;

    @Column(name = "sitting_title", length = 512)
    private String sittingTitle;

    @Column(name = "agenda_item_title", length = 1024)
    private String agendaItemTitle;

    @Column(columnDefinition = "text", nullable = false)
    private String text;

    @Column(name = "source_url", nullable = false, length = 512)
    private String sourceUrl;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
