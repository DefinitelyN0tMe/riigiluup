-- Parliamentary oversight: written questions (kirjalik küsimus) and interpellations (arupärimine)
-- an MP puts to a minister, plus whether/when the minister answered. Sourced from the Riigikogu
-- document register (/api/documents by documentType), linking a question to its answer via the shared
-- volume (case file). The Riigikogu site scatters these across a 160k-document register with no
-- per-MP or per-minister navigation; here they are browsable per MP with a response-time signal.
-- One row per question document; answers update the same row in place.
CREATE TABLE oversight_item (
    id                  UUID         PRIMARY KEY,
    external_id         VARCHAR(64)  NOT NULL UNIQUE,   -- the question document uuid
    kind                VARCHAR(24)  NOT NULL,          -- WRITTEN_QUESTION | INTERPELLATION
    title               TEXT         NOT NULL,
    submitted_on        DATE,
    answer_deadline     DATE,
    addressee_name      VARCHAR(256),                   -- the minister / target
    volume_external_id  VARCHAR(64),                    -- case file shared with the answer document
    membership_number   INTEGER,
    answered            BOOLEAN      NOT NULL DEFAULT FALSE,
    answer_external_id  VARCHAR(64),
    respondent_name     VARCHAR(256),
    responded_on        DATE,
    imported_at         TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_oversight_item_volume ON oversight_item (volume_external_id);
CREATE INDEX ix_oversight_item_submitted ON oversight_item (submitted_on);

-- The MP(s) who put the question (a question can have several co-enquirers). member_external_id is
-- the Riigikogu person id, matching plenary_member.external_id.
CREATE TABLE oversight_enquirer (
    id                  BIGSERIAL    PRIMARY KEY,
    oversight_item_id   UUID         NOT NULL REFERENCES oversight_item (id) ON DELETE CASCADE,
    member_external_id  VARCHAR(64)  NOT NULL,
    member_name         VARCHAR(256)
);
CREATE INDEX ix_oversight_enquirer_member ON oversight_enquirer (member_external_id);
CREATE INDEX ix_oversight_enquirer_item ON oversight_enquirer (oversight_item_id);
