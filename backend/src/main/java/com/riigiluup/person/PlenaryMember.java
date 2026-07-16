package com.riigiluup.person;

import com.riigiluup.source.SourceSnapshot;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "plenary_member")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlenaryMember {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    @Column(name = "first_name", nullable = false, length = 128)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 128)
    private String lastName;

    @Column(name = "full_name", nullable = false, length = 256)
    private String fullName;

    @Column(nullable = false, length = 256)
    private String slug;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "official_profile_url")
    private String officialProfileUrl;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "faction_external_id", length = 64)
    private String factionExternalId;

    @Column(name = "faction_name", length = 256)
    private String factionName;

    @Column(name = "electoral_district", length = 256)
    private String electoralDistrict;

    @Column(length = 16)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 256)
    private String email;

    @Column(name = "biography_html", columnDefinition = "text")
    private String biographyHtml;

    /** Education institutions (Wikidata P69), "; "-joined Estonian labels; nullable. */
    @Column(columnDefinition = "text")
    private String education;

    /** Other public offices held (Wikidata P39, "; "-joined Estonian labels); nullable. */
    @Column(columnDefinition = "text")
    private String positions;

    @Column(name = "parliament_seniority_days")
    private Integer parliamentSeniorityDays;

    /** Wikidata Q-ID (e.g. "Q123456"). Populated by WikidataImporter; nullable. */
    @Column(name = "wikidata_qid", length = 32)
    private String wikidataQid;

    @Column(name = "wikipedia_url_en", columnDefinition = "text")
    private String wikipediaUrlEn;

    @Column(name = "wikipedia_url_et", columnDefinition = "text")
    private String wikipediaUrlEt;

    @Column(name = "wikipedia_url_ru", columnDefinition = "text")
    private String wikipediaUrlRu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_snapshot_id")
    private SourceSnapshot sourceSnapshot;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
