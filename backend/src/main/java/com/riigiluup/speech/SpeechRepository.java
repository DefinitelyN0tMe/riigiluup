package com.riigiluup.speech;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SpeechRepository extends JpaRepository<Speech, Long> {

    Optional<Speech> findBySourceNameAndExternalId(String sourceName, String externalId);

    /**
     * Full-text search over speech texts. 'simple' config — PostgreSQL has no Estonian
     * stemmer, so matching is exact-wordform (documented in methodology). Excerpts are
     * built server-side: ts_headline marks hits with [[ ]] delimiters that the frontend
     * converts to elements itself (never raw HTML into the DOM). Null-safe casts follow
     * the project convention for optional filters.
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
                        THEN left(s.text, 320)
                        ELSE ts_headline('simple', s.text, plainto_tsquery('simple', cast(:q AS text)),
                                         'MaxFragments=2, MaxWords=30, MinWords=10, StartSel=[[, StopSel=]]')
                   END AS excerpt
            FROM speech s
            LEFT JOIN plenary_member pm ON pm.id = s.plenary_member_id
            WHERE (cast(:q AS text) IS NULL OR cast(:q AS text) = ''
                   OR s.tsv @@ plainto_tsquery('simple', cast(:q AS text)))
              AND (cast(:slug AS text) IS NULL OR pm.slug = cast(:slug AS text))
              AND (cast(:fromTs AS timestamptz) IS NULL OR s.spoken_at >= cast(:fromTs AS timestamptz))
              AND (cast(:toTs AS timestamptz) IS NULL OR s.spoken_at < cast(:toTs AS timestamptz))
            ORDER BY s.spoken_at DESC, s.id DESC
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
            """)
    Page<SpeechSearchRow> search(@Param("q") String q,
                                 @Param("slug") String slug,
                                 @Param("fromTs") Instant fromTs,
                                 @Param("toTs") Instant toTs,
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
}
