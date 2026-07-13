CREATE TABLE vote_faction_alignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vote_event_id UUID NOT NULL REFERENCES vote_event(id) ON DELETE CASCADE,
    faction_external_id VARCHAR(64) NOT NULL,
    majority_choice VARCHAR(32),
    majority_count INT NOT NULL DEFAULT 0,
    comparable_count INT NOT NULL DEFAULT 0,
    has_clear_majority BOOLEAN NOT NULL DEFAULT FALSE,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_vote_faction_alignment UNIQUE (vote_event_id, faction_external_id)
);

CREATE INDEX ix_vote_faction_alignment_event
    ON vote_faction_alignment (vote_event_id);
CREATE INDEX ix_vote_faction_alignment_faction
    ON vote_faction_alignment (faction_external_id);
