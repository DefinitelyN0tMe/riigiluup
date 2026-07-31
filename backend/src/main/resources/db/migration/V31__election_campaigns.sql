-- Broaden mp_election_result from "how the MP won their RK seat" to the MP's electoral
-- footprint across every published election on opendata.valimised.ee: RK_2023, EP_2024,
-- KOV_2021, KOV_2025. A campaign can now be a non-elected candidacy, so mandate_type is
-- optional and an explicit `elected` flag is added. Only high-confidence matches are stored
-- (a name unique in both the MP roster and the election); ambiguous names are left out
-- rather than guessed. Historical/immutable per election, still upserted by code.

ALTER TABLE mp_election_result ADD COLUMN elected BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE mp_election_result ALTER COLUMN mandate_type DROP NOT NULL;

-- Existing rows are all RK_2023 seated MPs: a directly-elected mandate means elected;
-- a SUBSTITUTE (asendusliige) entered mid-term and did not win at the election itself.
UPDATE mp_election_result
    SET elected = (mandate_type <> 'SUBSTITUTE')
    WHERE election_code = 'RK_2023';
