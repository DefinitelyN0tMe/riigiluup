package com.riigiluup.speech;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SpeechRepository extends JpaRepository<Speech, Long> {

    /** Per-sitting replace on re-import — the source has no per-event natural key. */
    long deleteBySourceNameAndSourceUrl(String sourceName, String sourceUrl);

    /** Lightweight (id, agenda title) projection for the link backfill — avoids loading text. */
    @Query("SELECT s.id AS id, s.agendaItemTitle AS agendaItemTitle FROM Speech s")
    List<SpeechAgendaRow> findAllAgendaTitles();

    interface SpeechAgendaRow {
        Long getId();
        String getAgendaItemTitle();
    }

    /**
     * Full-text search over speech texts. 'simple' config — PostgreSQL has no Estonian
     * stemmer, so matching is exact-wordform (documented in methodology). Excerpts are
     * built server-side: ts_headline marks hits with [[ ]] delimiters that the frontend
     * converts to elements itself (never raw HTML into the DOM). Null-safe casts follow
     * the project convention for optional filters. Ordering is chronological (oldest first)
     * when filtered to a bill, so a debate reads top to bottom; newest-first otherwise.
     * NB: no SQL line comments inside the query — Hibernate appends the LIMIT clause and a
     * trailing "--" comment would swallow its bind parameter.
     */
    @Query(nativeQuery = true, value = """
            SELECT s.id AS id,
                   s.speaker_raw AS speakerRaw,
                   s.spoken_at AS spokenAt,
                   s.sitting_title AS sittingTitle,
                   s.agenda_item_title AS agendaItemTitle,
                   s.source_url AS sourceUrl,
                   pm.slug AS memberSlug,
                   pm.full_name AS memberName,
                   CASE WHEN cast(:q AS text) IS NULL OR cast(:q AS text) = ''
                        THEN s.text
                        ELSE ts_headline('simple', s.text, plainto_tsquery('simple', cast(:q AS text)),
                                         'MaxFragments=2, MaxWords=30, MinWords=10, StartSel=[[, StopSel=]]')
                   END AS excerpt,
                   blink.bid AS billId,
                   blink.bmark AS billMark,
                   blink.btype AS billDraftType
            FROM speech s
            LEFT JOIN plenary_member pm ON pm.id = s.plenary_member_id
            LEFT JOIN LATERAL (
                   SELECT cast(li.id AS text) AS bid, li.mark AS bmark, li.draft_type_code AS btype
                   FROM speech_bill_link sbl
                   JOIN legislative_item li
                     ON li.mark = sbl.mark AND li.draft_type_code = sbl.draft_type_code
                    AND li.membership = s.membership
                   WHERE sbl.speech_id = s.id
                   ORDER BY li.mark
                   LIMIT 1
            ) blink ON true
            WHERE (cast(:q AS text) IS NULL OR cast(:q AS text) = ''
                   OR s.tsv @@ plainto_tsquery('simple', cast(:q AS text)))
              AND (cast(:slug AS text) IS NULL OR pm.slug = cast(:slug AS text))
              AND (cast(:fromTs AS timestamptz) IS NULL OR s.spoken_at >= cast(:fromTs AS timestamptz))
              AND (cast(:toTs AS timestamptz) IS NULL OR s.spoken_at < cast(:toTs AS timestamptz))
              AND (cast(:billMark AS int) IS NULL OR EXISTS (
                     SELECT 1 FROM speech_bill_link l
                     WHERE l.speech_id = s.id AND l.mark = cast(:billMark AS int)
                       AND l.draft_type_code = cast(:billDraftType AS text)))
              AND (cast(:billMembership AS int) IS NULL OR s.membership = cast(:billMembership AS int))
            ORDER BY
                CASE WHEN cast(:billMark AS int) IS NOT NULL THEN s.spoken_at END ASC,
                CASE WHEN cast(:billMark AS int) IS NULL     THEN s.spoken_at END DESC,
                s.id ASC
            """,
            countQuery = """
            SELECT count(*)
            FROM speech s
            LEFT JOIN plenary_member pm ON pm.id = s.plenary_member_id
            WHERE (cast(:q AS text) IS NULL OR cast(:q AS text) = ''
                   OR s.tsv @@ plainto_tsquery('simple', cast(:q AS text)))
              AND (cast(:slug AS text) IS NULL OR pm.slug = cast(:slug AS text))
              AND (cast(:fromTs AS timestamptz) IS NULL OR s.spoken_at >= cast(:fromTs AS timestamptz))
              AND (cast(:toTs AS timestamptz) IS NULL OR s.spoken_at < cast(:toTs AS timestamptz))
              AND (cast(:billMark AS int) IS NULL OR EXISTS (
                     SELECT 1 FROM speech_bill_link l
                     WHERE l.speech_id = s.id AND l.mark = cast(:billMark AS int)
                       AND l.draft_type_code = cast(:billDraftType AS text)))
              AND (cast(:billMembership AS int) IS NULL OR s.membership = cast(:billMembership AS int))
            """)
    Page<SpeechListRow> search(@Param("q") String q,
                               @Param("slug") String slug,
                               @Param("fromTs") Instant fromTs,
                               @Param("toTs") Instant toTs,
                               @Param("billMark") Integer billMark,
                               @Param("billDraftType") String billDraftType,
                               @Param("billMembership") Integer billMembership,
                               Pageable pageable);

    interface SpeechSearchRow {
        Long getId();
        String getSpeakerRaw();
        Instant getSpokenAt();
        String getSittingTitle();
        String getAgendaItemTitle();
        String getSourceUrl();
        String getMemberSlug();
        String getMemberName();
        String getExcerpt();
    }

    /** Search rows also carry the primary linked bill (if any) so each speech links back to it. */
    interface SpeechListRow extends SpeechSearchRow {
        String getBillId();
        Integer getBillMark();
        String getBillDraftType();
    }
}
