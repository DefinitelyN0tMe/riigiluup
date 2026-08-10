package com.riigiluup.election;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ElectionResultRepository extends JpaRepository<ElectionResult, UUID> {

    /** Every matched campaign of a member (RK / EP / KOV); ordered for display in the mapper. */
    List<ElectionResult> findByMemberExternalId(String memberExternalId);

    /** Rows for one election code — used to gate the per-code startup campaign load. */
    long countByElectionCode(String electionCode);

    /** Historical (Mölder-sourced, pre-2023) rows — used to gate the startup historical load. */
    long countByHistoricalTrue();

    /** Idempotent full-replace for the historical layer. */
    @Modifying
    @Query("delete from ElectionResult e where e.historical = true")
    void deleteAllHistorical();

    /**
     * Bulk delete so the rows are removed immediately, before the fresh inserts flush —
     * a derived deleteBy loads entities and Hibernate would order the new inserts ahead of
     * the queued deletes, violating the (member_external_id, election_code) unique constraint.
     */
    @Modifying
    @Query("delete from ElectionResult e where e.electionCode = :code")
    void deleteByElectionCode(@Param("code") String code);
}
