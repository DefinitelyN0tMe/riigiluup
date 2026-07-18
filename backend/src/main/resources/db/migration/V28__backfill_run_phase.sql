-- Richer progress for the full-seed backfill. The historical backfill grew from a
-- bills+votes window walk into a dependency-ordered pipeline over every importer
-- (members, groups, elections, wikidata, speeches, questions, initiatives, finance,
-- activity, RT links, …). A multi-year ALL run takes well over an hour, so ops need to
-- see WHICH phase is running and WHAT each step imported — the fixed bills/votes counters
-- no longer describe the run.
--
-- phase       — human-readable label of the step currently executing (e.g. "wikidata").
-- step_counts — JSON object of per-step upsert counts (e.g. {"members":101,"bills":1450}).
-- Both nullable: old bills/votes-only runs and older rows simply leave them empty.

ALTER TABLE backfill_run ADD COLUMN phase       TEXT;
ALTER TABLE backfill_run ADD COLUMN step_counts TEXT;
