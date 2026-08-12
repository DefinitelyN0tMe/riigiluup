-- Amendment proposals (muudatusettepanekud) to a bill, from the Riigikogu draft detail API's
-- `amendments` list. Each row is one proposal document: its title names the proposing faction/MP and
-- the bill, and it carries a public file (the actual amendment text). The Riigikogu site buries these
-- inside the bill's document pile; here they are listed on the bill. Full-replace per bill on each
-- detail refresh, mirroring legislative_stage.
CREATE TABLE bill_amendment (
    id                   UUID         PRIMARY KEY,
    legislative_item_id  UUID         NOT NULL REFERENCES legislative_item (id),
    external_id          VARCHAR(64),
    title                TEXT         NOT NULL,
    reference            VARCHAR(160),
    file_uuid            VARCHAR(64),
    file_name            VARCHAR(512),
    sequence             INTEGER      NOT NULL DEFAULT 0,
    imported_at          TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_bill_amendment_item ON bill_amendment (legislative_item_id);
