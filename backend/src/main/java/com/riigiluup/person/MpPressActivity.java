package com.riigiluup.person;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One press-activity entry ("Ajakirjandustegevus") of a sitting MP, from the Riigikogu detail API's
 * {@code press} list. {@code description} is the free-text line (article title plus publication(s)
 * and date); {@code url} points to the article itself (an external outlet, not riigikogu.ee) and may
 * be null when the source lists a mention without a link. {@code memberExternalId} is the Riigikogu
 * person id. Idempotent full-replace per member on each detail refresh; shown on the profile.
 */
@Entity
@Table(name = "mp_press_activity")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MpPressActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_external_id", nullable = false, length = 64)
    private String memberExternalId;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "url", length = 1024)
    private String url;

    @Column(name = "published_on")
    private LocalDate publishedOn;

    @Column(name = "membership_number")
    private Integer membershipNumber;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
