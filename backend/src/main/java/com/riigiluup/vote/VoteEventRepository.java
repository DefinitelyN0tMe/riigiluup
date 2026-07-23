package com.riigiluup.vote;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VoteEventRepository extends JpaRepository<VoteEvent, UUID> {

    Optional<VoteEvent> findBySourceNameAndExternalId(String sourceName, String externalId);

    /** Vote events since a given instant — drives the homepage "+N this week" delta. */
    long countByStartedAtAfter(Instant startedAt);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "legislativeItem")
    @Query("""
        select v from VoteEvent v
        where (cast(:from as instant) is null or v.startedAt >= :from)
          and (cast(:to as instant) is null or v.startedAt <= :to)
          and (cast(:type as string) is null or v.type = :type)
        order by v.startedAt desc, v.id desc
        """)
    Page<VoteEvent> search(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("type") VoteEventType type,
            Pageable pageable
    );

    /**
     * Rich search — same filters as basic search plus:
     * - hourOfDay (0..23, Europe/Tallinn)
     * - dowMon (0..6, Mon=0..Sun=6, Europe/Tallinn)
     * - onlyWeekend (true → only Sat/Sun)
     * - factionA & factionB → only events where those two factions had DIFFERENT clear majorities
     *   (useful for drilling into faction-agreement heatmap cells)
     * Native SQL because we need EXTRACT(...) and MOD().
     */
    @Query(value = """
        SELECT v.* FROM vote_event v
        WHERE (cast(:fromTs AS timestamp) IS NULL OR v.started_at >= cast(:fromTs AS timestamp))
          AND (cast(:toTs AS timestamp) IS NULL OR v.started_at <= cast(:toTs AS timestamp))
          AND (cast(:type AS text) IS NULL OR v.type = cast(:type AS text))
          AND (cast(:hour AS integer) IS NULL
                OR (v.started_at IS NOT NULL
                    AND EXTRACT(hour FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int
                        = cast(:hour AS integer)))
          AND (cast(:dow AS integer) IS NULL
                OR (v.started_at IS NOT NULL
                    AND MOD(EXTRACT(dow FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int + 6, 7)
                        = cast(:dow AS integer)))
          AND (cast(:onlyWeekend AS boolean) IS NULL OR NOT cast(:onlyWeekend AS boolean)
                OR (v.started_at IS NOT NULL
                    AND MOD(EXTRACT(dow FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int + 6, 7) >= 5))
          AND (cast(:nightOnly AS boolean) IS NULL OR NOT cast(:nightOnly AS boolean)
                OR (v.started_at IS NOT NULL
                    AND (EXTRACT(hour FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int < 8
                         OR EXTRACT(hour FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int >= 22)))
          AND (cast(:lateOnly AS boolean) IS NULL OR NOT cast(:lateOnly AS boolean)
                OR (v.started_at IS NOT NULL
                    AND (EXTRACT(hour FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int >= 22
                         OR EXTRACT(hour FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int < 6)))
          AND (cast(:factionA AS text) IS NULL OR cast(:factionB AS text) IS NULL OR EXISTS (
                SELECT 1 FROM vote_faction_alignment a
                JOIN vote_faction_alignment b ON b.vote_event_id = a.vote_event_id
                WHERE a.vote_event_id = v.id
                  AND a.faction_external_id = cast(:factionA AS text)
                  AND b.faction_external_id = cast(:factionB AS text)
                  AND a.has_clear_majority = TRUE AND b.has_clear_majority = TRUE
                  AND a.majority_choice IN ('FOR','AGAINST','ABSTAINED')
                  AND b.majority_choice IN ('FOR','AGAINST','ABSTAINED')
                  AND a.majority_choice <> b.majority_choice))
        ORDER BY v.started_at DESC NULLS LAST, v.id DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM vote_event v
        WHERE (cast(:fromTs AS timestamp) IS NULL OR v.started_at >= cast(:fromTs AS timestamp))
          AND (cast(:toTs AS timestamp) IS NULL OR v.started_at <= cast(:toTs AS timestamp))
          AND (cast(:type AS text) IS NULL OR v.type = cast(:type AS text))
          AND (cast(:hour AS integer) IS NULL
                OR (v.started_at IS NOT NULL
                    AND EXTRACT(hour FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int
                        = cast(:hour AS integer)))
          AND (cast(:dow AS integer) IS NULL
                OR (v.started_at IS NOT NULL
                    AND MOD(EXTRACT(dow FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int + 6, 7)
                        = cast(:dow AS integer)))
          AND (cast(:onlyWeekend AS boolean) IS NULL OR NOT cast(:onlyWeekend AS boolean)
                OR (v.started_at IS NOT NULL
                    AND MOD(EXTRACT(dow FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int + 6, 7) >= 5))
          AND (cast(:nightOnly AS boolean) IS NULL OR NOT cast(:nightOnly AS boolean)
                OR (v.started_at IS NOT NULL
                    AND (EXTRACT(hour FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int < 8
                         OR EXTRACT(hour FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int >= 22)))
          AND (cast(:lateOnly AS boolean) IS NULL OR NOT cast(:lateOnly AS boolean)
                OR (v.started_at IS NOT NULL
                    AND (EXTRACT(hour FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int >= 22
                         OR EXTRACT(hour FROM (v.started_at AT TIME ZONE 'Europe/Tallinn'))::int < 6)))
          AND (cast(:factionA AS text) IS NULL OR cast(:factionB AS text) IS NULL OR EXISTS (
                SELECT 1 FROM vote_faction_alignment a
                JOIN vote_faction_alignment b ON b.vote_event_id = a.vote_event_id
                WHERE a.vote_event_id = v.id
                  AND a.faction_external_id = cast(:factionA AS text)
                  AND b.faction_external_id = cast(:factionB AS text)
                  AND a.has_clear_majority = TRUE AND b.has_clear_majority = TRUE
                  AND a.majority_choice IN ('FOR','AGAINST','ABSTAINED')
                  AND b.majority_choice IN ('FOR','AGAINST','ABSTAINED')
                  AND a.majority_choice <> b.majority_choice))
        """,
        nativeQuery = true)
    Page<VoteEvent> richSearch(
            @Param("fromTs") Instant fromTs,
            @Param("toTs") Instant toTs,
            @Param("type") String type,
            @Param("hour") Integer hour,
            @Param("dow") Integer dow,
            @Param("onlyWeekend") Boolean onlyWeekend,
            @Param("nightOnly") Boolean nightOnly,
            @Param("lateOnly") Boolean lateOnly,
            @Param("factionA") String factionA,
            @Param("factionB") String factionB,
            Pageable pageable
    );

    /**
     * Paginated iteration over every VoteEvent by startedAt asc. Used by the alignment
     * backfill so we never hold the whole table in memory. Sort baked into the query
     * so the pageable only needs page/size (though Sort can still override).
     */
    @Query("select v from VoteEvent v order by v.startedAt asc nulls last, v.id asc")
    Slice<VoteEvent> findAllByStartedAtAsc(Pageable pageable);

    /**
     * Paginated iteration of VoteEvents that still need bill linking
     * (legislative_item still NULL). Ordered so the caller gets a stable slice window.
     */
    java.util.List<VoteEvent> findByLegislativeItemOrderByStartedAtAsc(
            com.riigiluup.legislation.LegislativeItem legislativeItem);

    @Query("select v from VoteEvent v where v.legislativeItem is null "
            + "order by v.startedAt asc nulls last, v.id asc")
    Slice<VoteEvent> findUnlinkedByStartedAtAsc(Pageable pageable);
}
