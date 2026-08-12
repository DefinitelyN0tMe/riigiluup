package com.riigiluup.legislation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface LegislativeItemRepository extends JpaRepository<LegislativeItem, UUID> {

    Optional<LegislativeItem> findBySourceNameAndExternalId(String sourceName, String externalId);

    /** Bills still under proceeding (not yet adopted/rejected/withdrawn) — homepage "in progress" delta. */
    long countByPhaseIn(Collection<LegislationPhase> phases);

    /** Ids of bills in the given phases — used to force an amendments detail-fetch for active bills. */
    @Query("select i.id from LegislativeItem i where i.phase in :phases")
    java.util.List<UUID> findIdsByPhaseIn(@Param("phases") Collection<LegislationPhase> phases);

    /**
     * Adopted laws (SE only — Riigikogu decisions publish in RT III with a different id
     * scheme) not yet linked to Riigi Teataja, with their publication date taken from the
     * AVALDATUD_RIIGITEATAJAS stage already stored by the bill importer. Newest first so
     * the daily batch covers fresh publications before the historical backlog.
     *
     * <p>{@code since} bounds the candidate set to acts published on/after that date; pass null
     * for no bound. Without it, a large-limit drain grinds decades of pre-2010 acts the RT search
     * mostly can't match — each costing an HTTP call — so the historical seed passes its own
     * {@code from} date here.
     */
    @Query(nativeQuery = true, value = """
        SELECT li.id AS id, li.title AS title,
               cast(max(s.occurred_at) AT TIME ZONE 'Europe/Tallinn' AS date) AS published
        FROM legislative_item li
        JOIN legislative_stage s ON s.legislative_item_id = li.id
             AND s.status_code = 'AVALDATUD_RIIGITEATAJAS'
        WHERE li.rt_act_id IS NULL AND li.draft_type_code = 'SE'
        GROUP BY li.id, li.title
        HAVING cast(:since AS date) IS NULL
            OR cast(max(s.occurred_at) AT TIME ZONE 'Europe/Tallinn' AS date) >= cast(:since AS date)
        ORDER BY published DESC
        LIMIT cast(:limit AS integer)
        """)
    java.util.List<Object[]> findRtLinkCandidates(
            @Param("since") java.time.LocalDate since, @Param("limit") int limit);

    /**
     * Rich search — every filter is null-safe. topicEdid drills into Eurovoc tags;
     * minDays/maxDays filter by (accepted_date − initiated_date) inclusive of both bounds,
     * and imply "must be ADOPTED with both dates known" — matches how the velocity chart
     * is computed. Native SQL because JPQL date arithmetic is awkward.
     */
    @Query(value = """
        SELECT i.* FROM legislative_item i
        WHERE (cast(:q AS text) IS NULL
                OR LOWER(i.title) LIKE LOWER(CONCAT('%', cast(:q AS text), '%'))
                OR cast(i.mark AS text) = cast(:q AS text))
          AND (cast(:phase AS text) IS NULL OR i.phase = cast(:phase AS text))
          AND (cast(:membership AS integer) IS NULL OR i.membership = cast(:membership AS integer))
          AND (cast(:topicEdid AS integer) IS NULL OR EXISTS (
                SELECT 1 FROM legislative_item_topic lit
                JOIN topic t ON t.id = lit.topic_id
                WHERE lit.legislative_item_id = i.id AND t.edid = cast(:topicEdid AS integer)))
          AND (cast(:minDays AS integer) IS NULL OR (
                i.accepted_date IS NOT NULL AND i.initiated_date IS NOT NULL
                AND (i.accepted_date - i.initiated_date) >= cast(:minDays AS integer)))
          AND (cast(:maxDays AS integer) IS NULL OR (
                i.accepted_date IS NOT NULL AND i.initiated_date IS NOT NULL
                AND (i.accepted_date - i.initiated_date) <= cast(:maxDays AS integer)))
          AND (cast(:committee AS text) IS NULL
                OR i.leading_committee_external_id = cast(:committee AS text))
        ORDER BY i.initiated_date DESC NULLS LAST, i.mark DESC NULLS LAST
        """,
        countQuery = """
        SELECT COUNT(*) FROM legislative_item i
        WHERE (cast(:q AS text) IS NULL
                OR LOWER(i.title) LIKE LOWER(CONCAT('%', cast(:q AS text), '%'))
                OR cast(i.mark AS text) = cast(:q AS text))
          AND (cast(:phase AS text) IS NULL OR i.phase = cast(:phase AS text))
          AND (cast(:membership AS integer) IS NULL OR i.membership = cast(:membership AS integer))
          AND (cast(:topicEdid AS integer) IS NULL OR EXISTS (
                SELECT 1 FROM legislative_item_topic lit
                JOIN topic t ON t.id = lit.topic_id
                WHERE lit.legislative_item_id = i.id AND t.edid = cast(:topicEdid AS integer)))
          AND (cast(:minDays AS integer) IS NULL OR (
                i.accepted_date IS NOT NULL AND i.initiated_date IS NOT NULL
                AND (i.accepted_date - i.initiated_date) >= cast(:minDays AS integer)))
          AND (cast(:maxDays AS integer) IS NULL OR (
                i.accepted_date IS NOT NULL AND i.initiated_date IS NOT NULL
                AND (i.accepted_date - i.initiated_date) <= cast(:maxDays AS integer)))
          AND (cast(:committee AS text) IS NULL
                OR i.leading_committee_external_id = cast(:committee AS text))
        """,
        nativeQuery = true)
    Page<LegislativeItem> search(
            @Param("q") String q,
            @Param("phase") String phase,
            @Param("membership") Integer membership,
            @Param("topicEdid") Integer topicEdid,
            @Param("minDays") Integer minDays,
            @Param("maxDays") Integer maxDays,
            @Param("committee") String committee,
            Pageable pageable
    );

    /** Bills where this committee is the lead — for the committee page count. */
    long countByLeadingCommitteeExternalId(String leadingCommitteeExternalId);

    /** Most recent bills a committee leads; newest initiated first. Limit via Pageable. */
    @Query("""
        select i from LegislativeItem i
        where i.leadingCommitteeExternalId = :committee
        order by i.initiatedDate desc nulls last, i.mark desc nulls last
        """)
    java.util.List<LegislativeItem> findRecentByLeadingCommittee(
            @Param("committee") String committee, Pageable pageable);
}
