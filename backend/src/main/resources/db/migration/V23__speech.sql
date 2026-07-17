-- Full speech texts from Riigikogu verbatim records (/api/steno/verbatims).
--
-- No natural per-event key exists in the source: the event `uuid` identifies the SPEAKER
-- (person uuid; the OpenAPI description "Sündmuse UUID" is wrong — verified against live
-- data). Re-imports therefore replace a sitting's speeches wholesale by source_url instead
-- of upserting per event; stenograms are edited after publication, so this also converges
-- texts with the source.
--
-- No source_snapshot rows for speeches: a single sitting week is megabytes of text and the
-- payload would be stored twice. Traceability comes from source_url on every row, which
-- points at the public stenogram page for the sitting.
--
-- Full-text search uses the 'simple' config: PostgreSQL has no Estonian stemmer, so we do
-- honest exact-wordform matching rather than pretending to stem. Documented in methodology.

CREATE TABLE speech (
    id                BIGSERIAL PRIMARY KEY,
    source_name       VARCHAR(64)  NOT NULL,
    speaker_uuid      VARCHAR(64),
    plenary_member_id UUID         REFERENCES plenary_member (id) ON DELETE SET NULL,
    speaker_raw       VARCHAR(255) NOT NULL,
    spoken_at         TIMESTAMPTZ  NOT NULL,
    sitting_title     VARCHAR(512),
    agenda_item_title VARCHAR(1024),
    text              TEXT         NOT NULL,
    source_url        VARCHAR(512) NOT NULL,
    imported_at       TIMESTAMPTZ  NOT NULL,
    tsv               TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', text)) STORED
);

CREATE INDEX ix_speech_tsv ON speech USING gin (tsv);
CREATE INDEX ix_speech_member_spoken ON speech (plenary_member_id, spoken_at DESC);
CREATE INDEX ix_speech_spoken_at ON speech (spoken_at DESC);
-- Supports the per-sitting replace on re-import.
CREATE INDEX ix_speech_sitting ON speech (source_name, source_url);
