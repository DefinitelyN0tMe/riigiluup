package com.riigiluup.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MpPartyMembershipRepository extends JpaRepository<MpPartyMembership, Long> {

    /** Oldest first; Postgres puts NULL start_date last for ASC — undated periods sink to the end. */
    List<MpPartyMembership> findByMemberExternalIdOrderByStartDateAsc(String memberExternalId);

    /**
     * Full refresh of the Wikidata-sourced rows, leaving any äriregister rows (phase 2) intact.
     *
     * <p>Bulk delete on purpose: a derived {@code deleteBySource} loads the rows and queues
     * {@code em.remove}s, which Hibernate flushes AFTER the re-inserts in the same transaction —
     * so a re-imported (member, party, start) row collides with the not-yet-deleted old one and
     * marks the transaction rollback-only. {@code flushAutomatically} runs the DELETE before those
     * inserts; {@code clearAutomatically} drops any now-stale managed rows from the context.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MpPartyMembership m where m.source = :source")
    void deleteBySource(@Param("source") String source);
}
