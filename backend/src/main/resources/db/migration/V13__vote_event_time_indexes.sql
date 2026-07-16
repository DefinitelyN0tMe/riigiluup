-- Functional indexes to keep VoteEventRepository.richSearch sargable when the
-- caller filters by hour or weekday. Without these, the EXTRACT(...) expressions
-- force a full table scan even when the caller narrows to e.g. hour=14.
--
-- Postgres NOTE: expression indexes must match the query's expression EXACTLY.
-- Both the query and the index use `AT TIME ZONE 'Europe/Tallinn'` and the same
-- casts (::int / ::integer are compatible).

CREATE INDEX IF NOT EXISTS ix_vote_event_hour_tallinn
    ON vote_event ((EXTRACT(hour FROM (started_at AT TIME ZONE 'Europe/Tallinn'))::int))
    WHERE started_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_vote_event_dow_tallinn
    ON vote_event ((MOD(EXTRACT(dow FROM (started_at AT TIME ZONE 'Europe/Tallinn'))::int + 6, 7)))
    WHERE started_at IS NOT NULL;
