-- Citizen initiatives from rahvaalgatus.ee (SA Eesti Koostöö Kogu).
-- One CSV request returns every initiative in every phase; we store all 17 source columns
-- verbatim, so each row is traceable to the source without a separate snapshot.
--
-- Municipal initiatives (destination = 'tallinn', 'narva-linn', …) are imported but never
-- displayed or counted: their signature threshold is 1% of residents, not the flat 1000 the
-- law sets for Riigikogu, so mixing them would corrupt every funnel denominator.
--
-- legislative_item_id is curated by an admin: the source carries no reference to the bill an
-- initiative turned into, and title-matching would be guesswork (initiative titles are
-- appeals, bill titles are legal names). NULL is the normal state, not a data defect.

CREATE TABLE initiative (
    id                        BIGSERIAL PRIMARY KEY,
    source_name               VARCHAR(64)  NOT NULL,
    external_id               VARCHAR(64)  NOT NULL,
    uuid                      VARCHAR(64),
    title                     TEXT,
    authors                   TEXT,
    destination               VARCHAR(64),
    phase                     VARCHAR(24) CHECK (phase IN
                                ('edit','sign','parliament','government','done')),
    published_at              TIMESTAMPTZ,
    signing_started_at        TIMESTAMPTZ,
    signing_ends_at           TIMESTAMPTZ,
    signature_count           INTEGER,
    last_signed_at            TIMESTAMPTZ,
    sent_to_parliament_at     TIMESTAMPTZ,
    parliament_decision       VARCHAR(32) CHECK (parliament_decision IN
                                ('return','reject','solve-differently','forward',
                                 'forward-to-government','draft-act-or-national-matter')),
    finished_in_parliament_at TIMESTAMPTZ,
    sent_to_government_at     TIMESTAMPTZ,
    finished_in_government_at TIMESTAMPTZ,
    legislative_item_id       UUID REFERENCES legislative_item (id) ON DELETE SET NULL,
    linked_by                 VARCHAR(255),
    linked_at                 TIMESTAMPTZ,
    imported_at               TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_initiative_external UNIQUE (source_name, external_id)
);

CREATE INDEX ix_initiative_destination_phase ON initiative (destination, phase);
CREATE INDEX ix_initiative_decision         ON initiative (parliament_decision);
CREATE INDEX ix_initiative_sent             ON initiative (sent_to_parliament_at DESC);
CREATE INDEX ix_initiative_legislative_item ON initiative (legislative_item_id);

-- Many-to-many: one initiative can be assigned to several committees. The source packs them
-- into a single CSV field separated by a newline INSIDE the quoted value ("social-affairs
-- \nenvironment"), which is also why the file has more physical lines than records.
CREATE TABLE initiative_committee (
    initiative_id  BIGINT      NOT NULL REFERENCES initiative (id) ON DELETE CASCADE,
    committee_slug VARCHAR(32) NOT NULL,
    group_id       UUID        REFERENCES "group" (id) ON DELETE SET NULL,
    PRIMARY KEY (initiative_id, committee_slug)
);

CREATE INDEX ix_initiative_committee_group ON initiative_committee (group_id);
