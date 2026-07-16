-- How each currently seated MP won their seat at a Riigikogu election: personal
-- votes, mandate type, district. Sourced from opendata.valimised.ee (CC BY 4.0),
-- matched to the MP by name. Historical/immutable per election, upserted by code.
CREATE TABLE mp_election_result (
    id                 UUID         PRIMARY KEY,
    member_external_id VARCHAR(64)  NOT NULL,
    election_code      VARCHAR(32)  NOT NULL,
    personal_votes     INT          NOT NULL,
    mandate_type       VARCHAR(32)  NOT NULL,
    district_number    INT,
    party_name         VARCHAR(256),
    ballot_number      INT,
    imported_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_mp_election UNIQUE (member_external_id, election_code)
);

CREATE INDEX idx_mp_election_member ON mp_election_result (member_external_id);
