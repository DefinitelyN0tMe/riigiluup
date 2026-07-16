-- Codename change politico → riigiluup. The seed in V12 attributed rows to
-- 'politico-curator'; V12 itself is left untouched (applied migrations are
-- immutable — editing it breaks checksum validation on existing databases).
-- This forward migration renames the attribution value on existing data, so new
-- and existing environments converge on 'riigiluup-curator'.
UPDATE mp_external_affiliation
SET verified_by = 'riigiluup-curator'
WHERE verified_by = 'politico-curator';
