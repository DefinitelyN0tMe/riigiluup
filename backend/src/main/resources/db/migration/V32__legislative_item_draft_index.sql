-- Speed up resolving a speech to its bill. The speech search (SpeechRepository.search) and the
-- bill-filter both join legislative_item on (mark, draft_type_code, membership) — once per matched
-- speech row via a LATERAL. Only an index on membership alone existed (V6), leaving each lookup to
-- scan all bills of that composition. This composite index turns it into a direct lookup.
CREATE INDEX ix_legislative_item_draft
    ON legislative_item (mark, draft_type_code, membership);
