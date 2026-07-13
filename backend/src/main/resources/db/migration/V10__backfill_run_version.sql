-- Optimistic locking for backfill_run so the orchestrator's status flips (e.g. cancel)
-- don't silently clobber concurrent updates from the runLoop.
ALTER TABLE backfill_run ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
