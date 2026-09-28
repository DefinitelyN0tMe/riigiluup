-- Newly initiated bills (stage ALGATATUD) and bills back in the chamber after a presidential veto
-- (UUESTI_ARUTAMINE) were mapped to OTHER; LegislationPhase now maps them to SUBMITTED / IN_READINGS.
-- Re-derive the stored phase for the rows already imported (a handful of rows).
UPDATE legislative_item SET phase = 'SUBMITTED'
 WHERE active_stage_source_code = 'ALGATATUD' AND phase = 'OTHER';
UPDATE legislative_item SET phase = 'IN_READINGS'
 WHERE active_stage_source_code = 'UUESTI_ARUTAMINE' AND phase = 'OTHER';
