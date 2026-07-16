-- Wikidata cross-references. Populated by WikidataImporter (SPARQL against
-- query.wikidata.org). CC0 license — free to reuse for any purpose.
--
-- We store the Q-ID plus per-language Wikipedia URLs so the frontend can
-- deep-link to the article in the user's chosen locale without a runtime
-- Wikipedia lookup.

ALTER TABLE plenary_member
    ADD COLUMN IF NOT EXISTS wikidata_qid       VARCHAR(32),
    ADD COLUMN IF NOT EXISTS wikipedia_url_en   TEXT,
    ADD COLUMN IF NOT EXISTS wikipedia_url_et   TEXT,
    ADD COLUMN IF NOT EXISTS wikipedia_url_ru   TEXT;

CREATE INDEX IF NOT EXISTS ix_plenary_member_wikidata_qid
    ON plenary_member (wikidata_qid) WHERE wikidata_qid IS NOT NULL;
