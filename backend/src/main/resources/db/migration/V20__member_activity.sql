-- Cached per-MP parliamentary activity over the current term: plenary speeches and
-- questions, plus interpellations and written questions the MP submitted. Recomputed
-- periodically from the Riigikogu API so the profile can read it without live calls.
CREATE TABLE member_activity (
    member_external_id VARCHAR(64) PRIMARY KEY,
    speeches           INT         NOT NULL,
    questions          INT         NOT NULL,
    interpellations    INT         NOT NULL,
    written_questions  INT         NOT NULL,
    computed_at        TIMESTAMPTZ NOT NULL
);
