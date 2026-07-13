CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE source_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_name VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    external_id VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    source_url TEXT,
    fetched_at TIMESTAMPTZ NOT NULL,
    source_updated_at TIMESTAMPTZ,
    processing_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    processing_error TEXT
);

CREATE UNIQUE INDEX ux_source_snapshot_natural
    ON source_snapshot (source_name, entity_type, external_id, payload_hash);

CREATE INDEX ix_source_snapshot_status
    ON source_snapshot (processing_status, fetched_at);

CREATE TABLE plenary_member (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(64) NOT NULL,
    source_name VARCHAR(64) NOT NULL,
    first_name VARCHAR(128) NOT NULL,
    last_name VARCHAR(128) NOT NULL,
    full_name VARCHAR(256) NOT NULL,
    slug VARCHAR(256) NOT NULL,
    photo_url TEXT,
    official_profile_url TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    faction_external_id VARCHAR(64),
    faction_name VARCHAR(256),
    source_snapshot_id UUID REFERENCES source_snapshot(id),
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_plenary_member_external UNIQUE (source_name, external_id),
    CONSTRAINT ux_plenary_member_slug UNIQUE (slug)
);

CREATE INDEX ix_plenary_member_active ON plenary_member (active);
CREATE INDEX ix_plenary_member_last_name ON plenary_member (last_name);

CREATE TABLE import_run_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_name VARCHAR(64) NOT NULL,
    job_name VARCHAR(128) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    records_seen INT NOT NULL DEFAULT 0,
    records_upserted INT NOT NULL DEFAULT 0,
    error_message TEXT
);
