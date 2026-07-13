CREATE TABLE vote_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(64) NOT NULL,
    source_name VARCHAR(64) NOT NULL,
    voting_number INT,
    type VARCHAR(32) NOT NULL,
    type_source_code VARCHAR(64),
    description TEXT,
    sitting_external_id VARCHAR(64),
    sitting_title VARCHAR(512),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    result_in_favor INT NOT NULL DEFAULT 0,
    result_against INT NOT NULL DEFAULT 0,
    result_abstained INT NOT NULL DEFAULT 0,
    result_neutral INT NOT NULL DEFAULT 0,
    result_present INT NOT NULL DEFAULT 0,
    result_absent INT NOT NULL DEFAULT 0,
    source_snapshot_id UUID REFERENCES source_snapshot(id),
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_vote_event_external UNIQUE (source_name, external_id)
);

CREATE INDEX ix_vote_event_started_at ON vote_event (started_at DESC);
CREATE INDEX ix_vote_event_type_started ON vote_event (type, started_at DESC);

CREATE TABLE individual_vote (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vote_event_id UUID NOT NULL REFERENCES vote_event(id) ON DELETE CASCADE,
    plenary_member_id UUID NOT NULL REFERENCES plenary_member(id) ON DELETE CASCADE,
    faction_external_id VARCHAR(64),
    faction_name VARCHAR(256),
    choice VARCHAR(32) NOT NULL,
    choice_source_code VARCHAR(64),
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_individual_vote UNIQUE (vote_event_id, plenary_member_id)
);

CREATE INDEX ix_individual_vote_member_event
    ON individual_vote (plenary_member_id, vote_event_id);
CREATE INDEX ix_individual_vote_faction ON individual_vote (faction_external_id);
