package com.riigiluup.activity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Cached per-MP parliamentary activity over the current term: plenary speeches and
 * questions (from the stenograms) plus interpellations and written questions the MP
 * submitted. Recomputed periodically from the Riigikogu API (a few hundred throttled
 * calls), so the profile can read it cheaply.
 */
@Entity
@Table(name = "member_activity")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberActivity {

    @Id
    @Column(name = "member_external_id", length = 64)
    private String memberExternalId;

    @Column(nullable = false)
    private int speeches;

    @Column(nullable = false)
    private int questions;

    @Column(nullable = false)
    private int interpellations;

    @Column(name = "written_questions", nullable = false)
    private int writtenQuestions;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}
