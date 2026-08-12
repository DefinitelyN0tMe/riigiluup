-- Press activity ("Ajakirjandustegevus") of a sitting MP: press mentions and authored articles the
-- Riigikogu detail API lists under `press`. Each row carries a free-text description (article title
-- plus the publication(s) and date), an external URL that points to the ARTICLE itself (not to
-- riigikogu.ee), a publication date and the parliamentary term it belongs to. member_external_id is
-- the Riigikogu person id (as in mp_faction_membership), not a FK. Idempotent full-replace per member
-- on each detail refresh, so no unique constraint is required.
CREATE TABLE mp_press_activity (
    id                 BIGSERIAL   PRIMARY KEY,
    member_external_id VARCHAR(64) NOT NULL,
    description        TEXT        NOT NULL,
    url                VARCHAR(1024),
    published_on       DATE,
    membership_number  INTEGER,
    imported_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_mp_press_activity_member ON mp_press_activity (member_external_id);
