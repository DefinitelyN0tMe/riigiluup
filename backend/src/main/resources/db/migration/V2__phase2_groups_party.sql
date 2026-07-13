-- Extend plenary_member with detail fields backfilled from /api/plenary-members/{uuid}.
ALTER TABLE plenary_member
    ADD COLUMN electoral_district VARCHAR(256),
    ADD COLUMN gender VARCHAR(16),
    ADD COLUMN date_of_birth DATE,
    ADD COLUMN email VARCHAR(256),
    ADD COLUMN biography_html TEXT,
    ADD COLUMN parliament_seniority_days INT;

-- Unified group table for fractions, committees, delegations, associations, etc.
CREATE TABLE "group" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(64) NOT NULL,
    source_name VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    name VARCHAR(512) NOT NULL,
    short_name VARCHAR(64),
    color_hex VARCHAR(16),
    secretariat_name VARCHAR(256),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    source_snapshot_id UUID REFERENCES source_snapshot(id),
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_group_external UNIQUE (source_name, external_id)
);

CREATE INDEX ix_group_type_active ON "group" (type, active);

-- Membership between MP and group with role and validity window.
CREATE TABLE group_membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plenary_member_id UUID NOT NULL REFERENCES plenary_member(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES "group"(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    start_date DATE,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    source_snapshot_id UUID REFERENCES source_snapshot(id),
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_group_membership UNIQUE (plenary_member_id, group_id, start_date)
);

CREATE INDEX ix_group_membership_member ON group_membership (plenary_member_id, active);
CREATE INDEX ix_group_membership_group  ON group_membership (group_id, active);

-- Parties are political parties (Reformierakond, EKRE, ...) distinct from Riigikogu factions.
CREATE TABLE party (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    short_name VARCHAR(32) NOT NULL,
    full_name VARCHAR(256) NOT NULL,
    color_hex VARCHAR(16),
    official_url TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ux_party_short_name UNIQUE (short_name)
);

-- Map a faction (Group with type=FRACTION) to a political party, with validity window.
CREATE TABLE faction_party_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    faction_external_id VARCHAR(64) NOT NULL,
    party_id UUID NOT NULL REFERENCES party(id) ON DELETE CASCADE,
    valid_from DATE,
    valid_to DATE,
    CONSTRAINT ux_faction_party_link UNIQUE (faction_external_id, party_id, valid_from)
);

CREATE INDEX ix_faction_party_link_faction ON faction_party_link (faction_external_id);
