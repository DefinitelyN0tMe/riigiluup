-- The Riigikogu verbatims API stamps every speech event with a bogus "+00:00" while the
-- wall-clock value is Tallinn LOCAL time (a 10:00 sitting opens at "10:00:09+00:00" in both
-- EET and EEST; the sitting-level date on the same payload is correct UTC). SpeechMapper used
-- to trust the offset, so every spoken_at was stored 2-3 hours late, which put speeches out of
-- order against votes (real UTC) on bill pages.
--
-- Re-zone the stored wall-clock as Europe/Tallinn, DST-aware per row:
--   stored 2026-09-17 10:00:09+00  ->  2026-09-17 07:00:09+00  (EEST, +3)
--   stored 2026-01-13 10:00:15+00  ->  2026-01-13 08:00:15+00  (EET,  +2)
-- Verified before running: the first-speech wall-clock of every sitting 2023-2026 clusters at
-- 10/12/14/15 (local sitting starts) with no 07/08 cluster, i.e. every row carries the same
-- defect, so a single uniform re-zone is correct. The importer is fixed in the same release,
-- so the 7-day re-import window writes correct values from now on.
SET LOCAL statement_timeout = '0';

UPDATE speech
SET spoken_at = (spoken_at AT TIME ZONE 'UTC') AT TIME ZONE 'Europe/Tallinn'
WHERE spoken_at IS NOT NULL;
