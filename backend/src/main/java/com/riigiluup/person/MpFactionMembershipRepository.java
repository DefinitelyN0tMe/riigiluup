package com.riigiluup.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MpFactionMembershipRepository extends JpaRepository<MpFactionMembership, Long> {

    /** A member's faction periods, oldest first — the profile renders them as a timeline. */
    List<MpFactionMembership> findByMemberExternalIdOrderByStartDateAsc(String memberExternalId);

    /**
     * Remove a member's rows before re-inserting from a fresh detail fetch (idempotent replace).
     * Bulk delete so it hits the DB immediately, ahead of the fresh inserts in the same tx —
     * otherwise Hibernate could order the inserts first and trip the (member, faction, start) unique.
     */
    @Modifying
    @Query("delete from MpFactionMembership f where f.memberExternalId = :ext")
    void deleteByMemberExternalId(@Param("ext") String ext);
}
