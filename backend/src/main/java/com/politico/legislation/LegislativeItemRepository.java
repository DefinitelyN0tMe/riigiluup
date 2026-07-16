package com.politico.legislation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LegislativeItemRepository extends JpaRepository<LegislativeItem, UUID> {

    Optional<LegislativeItem> findBySourceNameAndExternalId(String sourceName, String externalId);

    /**
     * Rich search — every filter is null-safe. topicEdid drills into Eurovoc tags;
     * minDays/maxDays filter by (accepted_date − initiated_date) inclusive of both bounds,
     * and imply "must be ADOPTED with both dates known" — matches how the velocity chart
     * is computed. Native SQL because JPQL date arithmetic is awkward.
     */
    @Query(value = """
        SELECT i.* FROM legislative_item i
        WHERE (cast(:q AS text) IS NULL
                OR LOWER(i.title) LIKE LOWER(CONCAT('%', cast(:q AS text), '%')))
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
        ORDER BY i.initiated_date DESC NULLS LAST, i.mark DESC NULLS LAST
        """,
        countQuery = """
        SELECT COUNT(*) FROM legislative_item i
        WHERE (cast(:q AS text) IS NULL
                OR LOWER(i.title) LIKE LOWER(CONCAT('%', cast(:q AS text), '%')))
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
        """,
        nativeQuery = true)
    Page<LegislativeItem> search(
            @Param("q") String q,
            @Param("phase") String phase,
            @Param("membership") Integer membership,
            @Param("topicEdid") Integer topicEdid,
            @Param("minDays") Integer minDays,
            @Param("maxDays") Integer maxDays,
            Pageable pageable
    );
}
