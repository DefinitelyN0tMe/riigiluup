-- Interpellations and written questions with government response dates, for the
-- "response latency" analytics: how long ministers take to answer, against the
-- legal deadline the source itself provides.
--
-- answered_date semantics differ by kind (both provided by the source):
--   WRITTEN_QUESTION → answerDocument.respondDate (written reply registered)
--   INTERPELLATION   → agendaItem.sittingDate (oral answer at a plenary sitting)
-- answer_deadline IS NULL marks volumes returned to the submitters ("tagastatud
-- esitajatele") — those are excluded from every latency denominator.

CREATE TABLE government_question (
    id              BIGSERIAL PRIMARY KEY,
    source_name     VARCHAR(64)  NOT NULL,
    external_id     VARCHAR(64)  NOT NULL,
    kind            VARCHAR(24)  NOT NULL CHECK (kind IN ('INTERPELLATION', 'WRITTEN_QUESTION')),
    mark            INTEGER,
    membership      INTEGER,
    title           TEXT,
    addressee_uuid  VARCHAR(64),
    addressee_name  VARCHAR(255),
    addressee_role  VARCHAR(512),
    submitting_date DATE NOT NULL,
    answer_deadline DATE,
    answered_date   DATE,
    imported_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_government_question_external UNIQUE (source_name, external_id)
);

CREATE INDEX ix_gov_question_kind_date ON government_question (kind, submitting_date DESC);
CREATE INDEX ix_gov_question_addressee ON government_question (addressee_name);
