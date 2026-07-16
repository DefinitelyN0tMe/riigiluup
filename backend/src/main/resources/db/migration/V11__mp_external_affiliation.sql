-- Hand-curated affiliations that are NOT part of Riigikogu's official record.
-- Used to surface e.g. Koos party membership of MPs who left their original faction.
-- Every row MUST cite an external source and a verifier.

CREATE TABLE mp_external_affiliation (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    member_slug       VARCHAR(256)  NOT NULL,
    organization      VARCHAR(256)  NOT NULL,           -- e.g. "Koos Erakond", "Parempoolsed"
    org_kind          VARCHAR(32)   NOT NULL,           -- PARTY | MOVEMENT | FRACTION_ORIGINAL | INDEPENDENT
    role              VARCHAR(256),                     -- e.g. "liige", "asutaja", "esimees", "kandidaat"
    valid_from        DATE          NOT NULL,
    valid_to          DATE,                             -- NULL = current
    source_url        TEXT          NOT NULL,
    source_label      VARCHAR(256)  NOT NULL,           -- e.g. "Postimees, 2024-03-15"
    verified_by       VARCHAR(128)  NOT NULL,           -- curator identifier
    verified_at       DATE          NOT NULL,
    note              TEXT,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX ix_mp_ext_aff_slug ON mp_external_affiliation (member_slug);
CREATE INDEX ix_mp_ext_aff_valid ON mp_external_affiliation (valid_from DESC);

COMMENT ON TABLE mp_external_affiliation IS
  'External (non-Riigikogu) party/movement affiliation timeline for MPs. Editorially curated. Every row cites a verifiable source.';
