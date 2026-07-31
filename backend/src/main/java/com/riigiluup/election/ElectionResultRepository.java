package com.riigiluup.election;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElectionResultRepository extends JpaRepository<ElectionResult, UUID> {

    /** The RK seat result — how the MP won their current seat. */
    Optional<ElectionResult> findFirstByMemberExternalIdOrderByElectionCodeDesc(String memberExternalId);

    /** Every matched campaign of a member (RK / EP / KOV); ordered for display in the mapper. */
    List<ElectionResult> findByMemberExternalId(String memberExternalId);

    /** Rows for the given election codes — used to gate the one-time startup campaign load. */
    long countByElectionCodeIn(Collection<String> electionCodes);

    /**
     * Bulk delete so the rows are removed immediately, before the fresh inserts flush —
     * a derived deleteBy loads entities and Hibernate would order the new inserts ahead of
     * the queued deletes, violating the (member_external_id, election_code) unique constraint.
     */
    @Modifying
    @Query("delete from ElectionResult e where e.electionCode = :code")
    void deleteByElectionCode(@Param("code") String code);
}
