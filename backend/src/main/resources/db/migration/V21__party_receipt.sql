-- Party income by year and income type, from the ERJK (party financing oversight)
-- open data — the "money in politics" source. Refreshed as a full replace from code.
CREATE TABLE party_receipt (
    id            UUID          PRIMARY KEY,
    erjk_party_id INT           NOT NULL,
    party_name    VARCHAR(256)  NOT NULL,
    category_id   VARCHAR(16)   NOT NULL,
    category_name VARCHAR(128)  NOT NULL,
    period_year   INT           NOT NULL,
    amount        NUMERIC(15,2) NOT NULL,
    imported_at   TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_party_receipt UNIQUE (erjk_party_id, category_id, period_year)
);

CREATE INDEX idx_party_receipt_party ON party_receipt (erjk_party_id);
