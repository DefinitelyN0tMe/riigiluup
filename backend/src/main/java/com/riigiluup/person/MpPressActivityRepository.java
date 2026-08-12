package com.riigiluup.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MpPressActivityRepository extends JpaRepository<MpPressActivity, Long> {

    /**
     * A member's press entries, newest first (undated rows last), for the profile list.
     * Ordered in SQL so the profile mapper can simply take the first N most recent.
     */
    @Query("select p from MpPressActivity p where p.memberExternalId = :ext "
            + "order by p.publishedOn desc nulls last, p.id desc")
    List<MpPressActivity> findByMemberNewestFirst(@Param("ext") String ext);

    long countByMemberExternalId(String memberExternalId);

    /**
     * Remove a member's rows before re-inserting from a fresh detail fetch (idempotent replace).
     * Bulk delete so it hits the DB immediately, ahead of the fresh inserts in the same tx.
     */
    @Modifying
    @Query("delete from MpPressActivity p where p.memberExternalId = :ext")
    void deleteByMemberExternalId(@Param("ext") String ext);
}
