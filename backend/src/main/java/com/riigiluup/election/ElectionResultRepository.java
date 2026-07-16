package com.riigiluup.election;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ElectionResultRepository extends JpaRepository<ElectionResult, UUID> {

    /** Most recent election result for a member (there is currently only RK_2023). */
    Optional<ElectionResult> findFirstByMemberExternalIdOrderByElectionCodeDesc(String memberExternalId);

    /**
     * Bulk delete so the rows are removed immediately, before the fresh inserts flush —
     * a derived deleteBy loads entities and Hibernate would order the new inserts ahead of
     * the queued deletes, violating the (member_external_id, election_code) unique constraint.
     */
    @Modifying
    @Query("delete from ElectionResult e where e.electionCode = :code")
    void deleteByElectionCode(@Param("code") String code);
}
