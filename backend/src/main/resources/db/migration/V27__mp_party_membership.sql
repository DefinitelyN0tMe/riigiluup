-- Party membership of a sitting MP over time. Sourced from Wikidata P102 ("member of
-- political party") with start/end qualifiers; matched to the MP by the same (name, DOB)
-- crossref the WikidataImporter already does. member_external_id is the Riigikogu person id
-- (as in mp_election_result) — not a FK. Party affiliation of an MP is public; only matched
-- sitting members are stored, never general party member lists.
--
-- NOT the same as faction (parliamentary group) or the party an MP ran for at the election —
-- those live on plenary_member / mp_election_result and are shown alongside, distinctly.
CREATE TABLE mp_party_membership (
    id                 BIGSERIAL PRIMARY KEY,
    member_external_id VARCHAR(64)  NOT NULL,
    party_qid          VARCHAR(32)  NOT NULL,
    party_label        VARCHAR(256) NOT NULL,
    start_date         DATE,
    end_date           DATE,
    source             VARCHAR(32)  NOT NULL DEFAULT 'wikidata',
    imported_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_mp_party_membership UNIQUE (member_external_id, party_qid, start_date, source)
);
CREATE INDEX ix_mp_party_membership_member ON mp_party_membership (member_external_id);
