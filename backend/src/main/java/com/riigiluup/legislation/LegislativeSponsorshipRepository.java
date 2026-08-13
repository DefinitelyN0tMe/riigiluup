package com.riigiluup.legislation;

import com.riigiluup.person.PlenaryMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LegislativeSponsorshipRepository
        extends JpaRepository<LegislativeSponsorship, UUID> {

    List<LegislativeSponsorship> findByLegislativeItem(LegislativeItem item);

    // Bulk JPQL delete — see LegislativeStageRepository for the Hibernate action-queue
    // ordering that makes a derived deleteBy hit the unique constraint on re-imports.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LegislativeSponsorship s where s.legislativeItem = :item")
    void deleteByLegislativeItem(@Param("item") LegislativeItem item);

    @Query("""
        select s.legislativeItem from LegislativeSponsorship s
        where s.plenaryMember = :member
        order by s.legislativeItem.initiatedDate desc nulls last
        """)
    Page<LegislativeItem> findItemsSponsoredByMember(
            @Param("member") PlenaryMember member, Pageable pageable);

    long countByPlenaryMember(PlenaryMember member);

    /** True once any sponsorship is linked to an MP — gates the one-time historical relink. */
    boolean existsByPlenaryMemberIsNotNull();

    /**
     * Backfill fix: link the individual-MP bill initiators that were stored as OTHER (the draft API
     * tags them type "user", which the classifier used to miss) to their plenary_member and set the
     * PLENARY_MEMBER kind. Only rows whose external id is an actual MP match (faction/committee ids
     * match the group table, not plenary_member), and only unlinked rows are touched, so it is safe
     * and idempotent. Own transaction so it can run from the startup backfill thread.
     */
    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query(value = """
        UPDATE legislative_sponsorship ls
        SET sponsor_kind = 'PLENARY_MEMBER', plenary_member_id = pm.id
        FROM plenary_member pm
        WHERE ls.external_id = pm.external_id AND ls.plenary_member_id IS NULL
        """, nativeQuery = true)
    int relinkMpSponsorships();

    /** Reclassify OTHER sponsorships whose external id is actually a committee group -> COMMITTEE, so
     *  they render a link to the committee page. Idempotent. */
    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query(value = """
        UPDATE legislative_sponsorship ls
        SET sponsor_kind = 'COMMITTEE'
        FROM "group" g
        WHERE g.external_id = ls.external_id
          AND g.type IN ('STANDING_COMMITTEE', 'SPECIAL_COMMITTEE')
          AND ls.sponsor_kind = 'OTHER'
        """, nativeQuery = true)
    int relinkCommitteeSponsors();
}
