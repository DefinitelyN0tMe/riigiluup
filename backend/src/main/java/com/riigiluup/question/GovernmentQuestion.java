package com.riigiluup.question;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * An interpellation or written question with the government's response timing.
 * answerDeadline == null marks a volume returned to the submitters — excluded
 * from every latency denominator.
 */
@Entity
@Table(name = "government_question")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GovernmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private QuestionKind kind;

    private Integer mark;

    private Integer membership;

    @Column(columnDefinition = "text")
    private String title;

    @Column(name = "addressee_uuid", length = 64)
    private String addresseeUuid;

    @Column(name = "addressee_name", length = 255)
    private String addresseeName;

    @Column(name = "addressee_role", length = 512)
    private String addresseeRole;

    @Column(name = "submitting_date", nullable = false)
    private LocalDate submittingDate;

    @Column(name = "answer_deadline")
    private LocalDate answerDeadline;

    @Column(name = "answered_date")
    private LocalDate answeredDate;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
