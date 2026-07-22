-- Tighten ux_mp_party_membership the same way V16 tightened ux_group_membership.
--
-- start_date is NULL for undated Wikidata P102 statements, and Postgres treats NULLs as
-- DISTINCT in a plain UNIQUE — so the same undated (member, party) membership could be
-- inserted twice across re-imports. PG15+ `NULLS NOT DISTINCT` makes the constraint reject
-- the duplicate instead.

-- Drop any pre-existing NULL-start_date duplicates first, keeping one arbitrary row,
-- so the tightened constraint can be added.
DELETE FROM mp_party_membership a
USING mp_party_membership b
WHERE a.ctid > b.ctid
  AND a.member_external_id = b.member_external_id
  AND a.party_qid = b.party_qid
  AND a.source = b.source
  AND a.start_date IS NOT DISTINCT FROM b.start_date;

ALTER TABLE mp_party_membership DROP CONSTRAINT ux_mp_party_membership;
ALTER TABLE mp_party_membership
  ADD CONSTRAINT ux_mp_party_membership
  UNIQUE NULLS NOT DISTINCT (member_external_id, party_qid, start_date, source);
