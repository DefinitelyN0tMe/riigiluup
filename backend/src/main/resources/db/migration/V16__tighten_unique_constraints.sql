-- Tighten upsert keys that contain a NULL column.
--
-- Postgres treats NULLs as DISTINCT in a plain UNIQUE, so a NULL-start_date committee membership
-- could be inserted twice under concurrent imports (6-hourly refresh overlapping a backfill). The
-- duplicate then permanently breaks that MP's detail refresh, because the reconcile finder returns
-- an Optional and throws on two rows. PG15+ `NULLS NOT DISTINCT` makes the constraint reject the
-- duplicate insert instead (the losing tx rolls back and retries, as with every other importer).
--
-- Deliberately NOT applied to legislative_sponsorship: its external_id is legitimately NULL for
-- multiple distinct government/committee sponsors on the same bill, and its reconcile is a full
-- delete+reinsert per bill (so any duplicate self-heals). NULLS NOT DISTINCT there would wrongly
-- collapse distinct organ sponsors into one.

-- Drop any pre-existing (member, group, start_date) duplicates first, keeping one arbitrary row,
-- so the tightened constraint can be added.
DELETE FROM group_membership a
USING group_membership b
WHERE a.ctid > b.ctid
  AND a.plenary_member_id = b.plenary_member_id
  AND a.group_id = b.group_id
  AND a.start_date IS NOT DISTINCT FROM b.start_date;

ALTER TABLE group_membership DROP CONSTRAINT ux_group_membership;
ALTER TABLE group_membership
  ADD CONSTRAINT ux_group_membership
  UNIQUE NULLS NOT DISTINCT (plenary_member_id, group_id, start_date);

-- Curated external-affiliation table had no natural key and no org_kind constraint: give it both
-- so an admin double-submit can't duplicate a timeline row and a typo'd kind can't slip through.
CREATE UNIQUE INDEX ux_mp_ext_aff_natural
  ON mp_external_affiliation (member_slug, organization, valid_from);

ALTER TABLE mp_external_affiliation
  ADD CONSTRAINT ck_mp_ext_aff_org_kind
  CHECK (org_kind IN ('PARTY', 'MOVEMENT', 'FRACTION_ORIGINAL', 'INDEPENDENT'));
