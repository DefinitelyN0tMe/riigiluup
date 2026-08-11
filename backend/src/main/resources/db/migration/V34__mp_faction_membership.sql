-- Faction (parliamentary group) membership of a sitting MP over time, so the profile can show
-- when an MP joined/left a faction rather than only their current one. Sourced from the Riigikogu
-- detail API (/api/plenary-members/{uuid}): each current-term faction carries a membership span
-- with start/end dates. member_external_id is the Riigikogu person id (as in mp_election_result),
-- not a FK. Idempotent full-replace per member on each detail refresh.
--
-- Distinct from mp_party_membership (Wikidata party affiliation) and from the party an MP ran for
-- at the election — a faction is the in-parliament group and is what changes when an MP leaves.
CREATE TABLE mp_faction_membership (
    id                  BIGSERIAL    PRIMARY KEY,
    member_external_id  VARCHAR(64)  NOT NULL,
    faction_external_id VARCHAR(64)  NOT NULL,
    faction_name        VARCHAR(256) NOT NULL,
    start_date          DATE,
    end_date            DATE,
    imported_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_mp_faction_membership UNIQUE (member_external_id, faction_external_id, start_date)
);
CREATE INDEX ix_mp_faction_membership_member ON mp_faction_membership (member_external_id);
