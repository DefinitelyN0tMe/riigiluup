-- Link plenary speeches to the bill (eelnõu) they debate.
--
-- The Riigikogu steno feed carries no structured draft reference on an agenda item (its
-- agendaItemUuid is a label for procedural items, and the VOTING_EVENT it nests has no
-- joinable id). But every bill-reading agenda-item title names the draft in parentheses,
-- e.g. "...seaduse eelnõu (644 SE) teine lugemine". We already hold the same key on the
-- bill: legislative_item.mark (644) + draft_type_code (SE) + membership (composition).
-- So we parse the (mark, type) codes out of the agenda title at ingest and resolve them to
-- a bill at query time on (mark, type, membership). Procedural / infotund / interpellation
-- items carry no code and correctly get no link; an over-parsed code that maps to no bill is
-- simply inert. Undercoverage (a debate under a non-standard title) is acceptable — a false
-- link is not, and this method never produces one.

-- The membership backfill below is a one-off bulk UPDATE over the whole speech table; on a
-- populated database it can exceed the app connection's statement_timeout. Disable the timeout
-- for this migration transaction only (SET LOCAL is scoped to it) so the backfill can complete.
SET LOCAL statement_timeout = '0';

ALTER TABLE speech ADD COLUMN membership INTEGER;

-- Backfill membership from the sitting date (Riigikogu composition boundaries), in a single
-- pass. Ongoing imports set it straight from the verbatim feed; this covers rows already
-- ingested so the bill<->debate join can match them.
UPDATE speech SET membership = CASE
        WHEN spoken_at >= TIMESTAMPTZ '2023-04-17 00:00:00+03' THEN 15
        WHEN spoken_at >= TIMESTAMPTZ '2019-04-04 00:00:00+03' THEN 14
        WHEN spoken_at >= TIMESTAMPTZ '2015-03-30 00:00:00+02' THEN 13
        ELSE 12
    END
    WHERE membership IS NULL;

CREATE INDEX ix_speech_membership ON speech (membership);

CREATE TABLE speech_bill_link (
    id              BIGSERIAL   PRIMARY KEY,
    speech_id       BIGINT      NOT NULL REFERENCES speech (id) ON DELETE CASCADE,
    mark            INTEGER     NOT NULL,
    draft_type_code VARCHAR(16) NOT NULL,
    CONSTRAINT ux_speech_bill_link UNIQUE (speech_id, mark, draft_type_code)
);

-- Bill -> speeches lookup (the detail page); speech -> links cleanup rides the FK cascade.
CREATE INDEX ix_speech_bill_link_draft ON speech_bill_link (mark, draft_type_code);
