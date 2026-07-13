-- Indexes on the source_snapshot_id FKs. Without these the retention job
-- (see SnapshotRetentionJob) forces a seq-scan on each dependent table to
-- verify no rows still reference a snapshot being NULL'd out.
CREATE INDEX ix_plenary_member_source_snapshot ON plenary_member (source_snapshot_id);
CREATE INDEX ix_group_source_snapshot ON "group" (source_snapshot_id);
CREATE INDEX ix_group_membership_source_snapshot ON group_membership (source_snapshot_id);
CREATE INDEX ix_vote_event_source_snapshot ON vote_event (source_snapshot_id);
CREATE INDEX ix_legislative_item_source_snapshot ON legislative_item (source_snapshot_id);

-- Retention purges old source_snapshot rows; dependent FKs must nullify, not RESTRICT.
-- Constraint names below are the PostgreSQL auto-generated defaults from V1/V2/V4/V6.
ALTER TABLE plenary_member
    DROP CONSTRAINT IF EXISTS plenary_member_source_snapshot_id_fkey,
    ADD CONSTRAINT plenary_member_source_snapshot_id_fkey
        FOREIGN KEY (source_snapshot_id) REFERENCES source_snapshot(id) ON DELETE SET NULL;

ALTER TABLE "group"
    DROP CONSTRAINT IF EXISTS group_source_snapshot_id_fkey,
    ADD CONSTRAINT group_source_snapshot_id_fkey
        FOREIGN KEY (source_snapshot_id) REFERENCES source_snapshot(id) ON DELETE SET NULL;

ALTER TABLE group_membership
    DROP CONSTRAINT IF EXISTS group_membership_source_snapshot_id_fkey,
    ADD CONSTRAINT group_membership_source_snapshot_id_fkey
        FOREIGN KEY (source_snapshot_id) REFERENCES source_snapshot(id) ON DELETE SET NULL;

ALTER TABLE vote_event
    DROP CONSTRAINT IF EXISTS vote_event_source_snapshot_id_fkey,
    ADD CONSTRAINT vote_event_source_snapshot_id_fkey
        FOREIGN KEY (source_snapshot_id) REFERENCES source_snapshot(id) ON DELETE SET NULL;

ALTER TABLE legislative_item
    DROP CONSTRAINT IF EXISTS legislative_item_source_snapshot_id_fkey,
    ADD CONSTRAINT legislative_item_source_snapshot_id_fkey
        FOREIGN KEY (source_snapshot_id) REFERENCES source_snapshot(id) ON DELETE SET NULL;
