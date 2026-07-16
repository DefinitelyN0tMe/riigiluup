-- One-time correction of a timezone ingestion bug.
--
-- Riigikogu emits offset-less LOCAL Estonian timestamps (e.g. "2026-06-01T15:00:00" = 15:00
-- Tallinn). Earlier mappers parsed these as UTC, so every stored instant is 2 h (winter) / 3 h
-- (summer) too late. The mappers now parse in Europe/Tallinn; this migration reinterprets the
-- already-stored (wrong) TIMESTAMPTZ values to the correct absolute instant.
--
-- Correctness/ordering: Flyway runs this exactly once, at startup, before the fixed importers
-- write any new (already-correct) rows. Re-imports of the same events afterwards overwrite these
-- values with freshly-correct ones (idempotent), so there is no risk of a double correction. On a
-- fresh/empty database every statement simply affects 0 rows.

UPDATE vote_event
SET started_at = (started_at AT TIME ZONE 'UTC') AT TIME ZONE 'Europe/Tallinn'
WHERE started_at IS NOT NULL;

UPDATE vote_event
SET ended_at = (ended_at AT TIME ZONE 'UTC') AT TIME ZONE 'Europe/Tallinn'
WHERE ended_at IS NOT NULL;

UPDATE legislative_stage
SET occurred_at = (occurred_at AT TIME ZONE 'UTC') AT TIME ZONE 'Europe/Tallinn'
WHERE occurred_at IS NOT NULL;

UPDATE legislative_item
SET amendments_deadline = (amendments_deadline AT TIME ZONE 'UTC') AT TIME ZONE 'Europe/Tallinn'
WHERE amendments_deadline IS NOT NULL;
