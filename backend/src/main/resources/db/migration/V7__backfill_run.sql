CREATE TABLE backfill_run (
    id UUID PRIMARY KEY,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at TIMESTAMPTZ,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    current_window_start DATE NOT NULL,
    kinds TEXT NOT NULL,
    status TEXT NOT NULL,
    bills_imported INT NOT NULL DEFAULT 0,
    votes_imported INT NOT NULL DEFAULT 0,
    windows_completed INT NOT NULL DEFAULT 0,
    windows_total INT NOT NULL,
    error_message TEXT
);

CREATE INDEX ix_backfill_run_status ON backfill_run (status);
CREATE INDEX ix_backfill_run_started_at ON backfill_run (started_at DESC);
