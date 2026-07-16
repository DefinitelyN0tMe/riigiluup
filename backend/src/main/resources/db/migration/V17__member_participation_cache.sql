-- Persist the last successful attendance (participation) figure per MP so the profile metric
-- never flips to "0 of 0" when the live Riigikogu statistics API is temporarily unavailable
-- (it is rate-limited whenever a backfill is running). Written on every successful live fetch,
-- read back as the fallback whenever the live fetch fails or returns an empty result.
CREATE TABLE member_participation_cache (
    member_external_id VARCHAR(64)      PRIMARY KEY,
    sittings           INT              NOT NULL,
    attended           INT              NOT NULL,
    rate               DOUBLE PRECISION NOT NULL,
    period_from        DATE,
    period_to          DATE,
    fetched_at         TIMESTAMPTZ      NOT NULL
);
