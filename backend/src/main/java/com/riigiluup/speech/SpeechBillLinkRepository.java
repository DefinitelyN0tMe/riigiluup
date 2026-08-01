package com.riigiluup.speech;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeechBillLinkRepository extends JpaRepository<SpeechBillLink, Long> {
    // Per-sitting cleanup rides the DB-level ON DELETE CASCADE on the bulk speech delete.
}
