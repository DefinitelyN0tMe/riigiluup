package com.riigiluup.speech;

import jakarta.persistence.*;
import lombok.*;

/**
 * A speech's reference to a debated bill, parsed from the agenda-item title as a draft code
 * (mark + type). Not an FK to legislative_item: the bill may be ingested after the speech, so
 * we store the natural key and resolve it to a bill at query time on (mark, type, membership).
 * Rows are removed with their speech via the DB-level ON DELETE CASCADE (the per-sitting
 * re-import deletes speeches in bulk, bypassing JPA orphan removal).
 */
@Entity
@Table(name = "speech_bill_link")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SpeechBillLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "speech_id", nullable = false)
    private Long speechId;

    @Column(name = "mark", nullable = false)
    private Integer mark;

    @Column(name = "draft_type_code", nullable = false, length = 16)
    private String draftTypeCode;
}
