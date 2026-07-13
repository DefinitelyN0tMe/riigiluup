CREATE TABLE legislative_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(64) NOT NULL,
    source_name VARCHAR(64) NOT NULL,
    mark INT,
    membership INT,
    title VARCHAR(1024) NOT NULL,
    initial_title VARCHAR(1024),
    draft_type_code VARCHAR(16),
    phase VARCHAR(32) NOT NULL,
    active_stage_source_code VARCHAR(64),
    active_status_source_code VARCHAR(64),
    proceeding_status VARCHAR(32),
    active_status_date DATE,
    introduction TEXT,
    initiated_date DATE,
    accepted_date DATE,
    amendments_deadline TIMESTAMPTZ,
    leading_committee_external_id VARCHAR(64),
    leading_committee_name VARCHAR(256),
    source_snapshot_id UUID REFERENCES source_snapshot(id),
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_legislative_item_external UNIQUE (source_name, external_id)
);
CREATE INDEX ix_legislative_item_initiated ON legislative_item (initiated_date DESC);
CREATE INDEX ix_legislative_item_phase ON legislative_item (phase);
CREATE INDEX ix_legislative_item_membership ON legislative_item (membership);

CREATE TABLE legislative_stage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legislative_item_id UUID NOT NULL REFERENCES legislative_item(id) ON DELETE CASCADE,
    reading_code VARCHAR(64),
    status_code VARCHAR(64),
    occurred_at TIMESTAMPTZ,
    sequence INT NOT NULL,
    CONSTRAINT ux_legislative_stage UNIQUE (legislative_item_id, sequence)
);
CREATE INDEX ix_legislative_stage_item ON legislative_stage (legislative_item_id);

CREATE TABLE legislative_sponsorship (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legislative_item_id UUID NOT NULL REFERENCES legislative_item(id) ON DELETE CASCADE,
    sponsor_kind VARCHAR(32) NOT NULL,
    plenary_member_id UUID REFERENCES plenary_member(id) ON DELETE SET NULL,
    external_id VARCHAR(64),
    display_name VARCHAR(512),
    CONSTRAINT ux_legislative_sponsorship UNIQUE (legislative_item_id, external_id)
);
CREATE INDEX ix_legislative_sponsorship_member
    ON legislative_sponsorship (plenary_member_id);

CREATE TABLE topic (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_name VARCHAR(64) NOT NULL DEFAULT 'eurovoc',
    edid INT NOT NULL,
    text VARCHAR(512) NOT NULL,
    CONSTRAINT ux_topic_source_edid UNIQUE (source_name, edid)
);
CREATE INDEX ix_topic_text ON topic (text);

CREATE TABLE legislative_item_topic (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legislative_item_id UUID NOT NULL REFERENCES legislative_item(id) ON DELETE CASCADE,
    topic_id UUID NOT NULL REFERENCES topic(id) ON DELETE CASCADE,
    CONSTRAINT ux_legislative_item_topic UNIQUE (legislative_item_id, topic_id)
);
CREATE INDEX ix_legislative_item_topic_topic ON legislative_item_topic (topic_id);

ALTER TABLE vote_event
    ADD COLUMN legislative_item_id UUID REFERENCES legislative_item(id) ON DELETE SET NULL;
CREATE INDEX ix_vote_event_legislative_item ON vote_event (legislative_item_id);
