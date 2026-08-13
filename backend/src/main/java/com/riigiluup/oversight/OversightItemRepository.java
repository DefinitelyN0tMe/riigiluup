package com.riigiluup.oversight;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OversightItemRepository extends JpaRepository<OversightItem, UUID> {

    Optional<OversightItem> findByExternalId(String externalId);

    /** Cheap existence checks so a resumed backfill skips questions/answers it already has. */
    boolean existsByExternalId(String externalId);

    boolean existsByAnswerExternalId(String answerExternalId);

    /** Newest unanswered-or-answered questions this MP (co-)put to a minister. */
    @Query("""
        select oi from OversightItem oi
        where oi.id in (select e.oversightItemId from OversightEnquirer e where e.memberExternalId = :ext)
        order by oi.submittedOn desc nulls last
        """)
    List<OversightItem> findByEnquirer(@Param("ext") String ext);

    /** All questions sharing a volume (case file) — used to attach an answer to its question. */
    List<OversightItem> findByVolumeExternalId(String volumeExternalId);

    long countByMembershipNumber(Integer membershipNumber);
}
