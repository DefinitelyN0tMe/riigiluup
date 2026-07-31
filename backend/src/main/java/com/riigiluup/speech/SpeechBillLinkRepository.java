package com.riigiluup.speech;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeechBillLinkRepository extends JpaRepository<SpeechBillLink, Long> {

    /** Explicit cleanup for the per-sitting re-import path (the DB cascade covers bulk deletes). */
    long deleteBySpeechId(Long speechId);
}
