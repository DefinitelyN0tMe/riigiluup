-- Historical pre-2023 Riigikogu electoral history, sourced from a dataset compiled by
-- Martin Mölder (Johan Skytte Institute, University of Tartu) covering RK candidates and
-- results 1992-2019 (with birth dates). Used with his permission and attributed on display.
-- These rows extend mp_election_result beyond the open-data footprint (RK_2023 / EP / KOV):
--   * historical = TRUE marks a Mölder-sourced pre-2023 candidacy (shown in a separate,
--     labelled block with the required source note), keeping it distinct from the open-data
--     rows that carry no such flag.
--   * district_name holds the named district (historical districts have no stable numbering
--     across years, so the name is the meaningful locator; district_number stays null here).

SET LOCAL statement_timeout = '0';

ALTER TABLE mp_election_result ADD COLUMN historical BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE mp_election_result ADD COLUMN district_name VARCHAR(256);
