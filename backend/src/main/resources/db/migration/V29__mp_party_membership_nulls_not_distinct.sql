-- Tighten ux_mp_party_membership the same way V16 tightened ux_group_membership.
--
-- start_date is NULL for undated Wikidata P102 statements, and Postgres treats NULLs as
-- DISTINCT in a plain UNIQUE — so the same undated (member, party) membership could be
-- inserted twice across re-imports. PG15+ `NULLS NOT DISTINCT` makes the constraint reject
-- the duplicate instead.

-- Drop any pre-existing NULL-start_date duplicates first so the tightened constraint
-- can be added. Prefer the row that carries an end_date (same tie-break as the
-- importer's dedupeForUniqueKey), falling back to physical order.
DELETE FROM mp_party_membership
WHERE ctid IN (
  SELECT ctid FROM (
    SELECT ctid, ROW_NUMBER() OVER (
      PARTITION BY member_external_id, party_qid, source, start_date
      ORDER BY (end_date IS NOT NULL) DESC, ctid
    ) AS rn
    FROM mp_party_membership
  ) ranked
  WHERE ranked.rn > 1
);

ALTER TABLE mp_party_membership DROP CONSTRAINT ux_mp_party_membership;
ALTER TABLE mp_party_membership
  ADD CONSTRAINT ux_mp_party_membership
  UNIQUE NULLS NOT DISTINCT (member_external_id, party_qid, start_date, source);
