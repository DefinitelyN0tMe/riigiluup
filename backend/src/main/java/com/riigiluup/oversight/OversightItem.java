package com.riigiluup.oversight;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One parliamentary oversight act: a written question or an interpellation an MP put to a minister,
 * with whether/when it was answered. Answers are matched to the question by the shared
 * {@code volumeExternalId} (the Riigikogu case file). Enquirers live in {@link OversightEnquirer}.
 */
@Entity
@Table(name = "oversight_item")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OversightItem {

    public enum Kind { WRITTEN_QUESTION, INTERPELLATION }

    @Id
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 64, unique = true)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 24)
    private Kind kind;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    private String title;

    @Column(name = "submitted_on")
    private LocalDate submittedOn;

    @Column(name = "answer_deadline")
    private LocalDate answerDeadline;

    @Column(name = "addressee_name", length = 256)
    private String addresseeName;

    @Column(name = "volume_external_id", length = 64)
    private String volumeExternalId;

    @Column(name = "membership_number")
    private Integer membershipNumber;

    @Column(name = "answered", nullable = false)
    private boolean answered;

    @Column(name = "answer_external_id", length = 64)
    private String answerExternalId;

    @Column(name = "respondent_name", length = 256)
    private String respondentName;

    @Column(name = "responded_on")
    private LocalDate respondedOn;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
