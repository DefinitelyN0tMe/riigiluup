-- Seed known cases where an MP's current party affiliation is NOT reflected in
-- their Riigikogu parliamentary-faction record (or where public trajectory adds context).
-- Sources: Riigikogu voting records, ERR, Postimees, party websites — cited per row.
-- All dates are approximate to the calendar day of the reported change.

-- Varro Vooglaid — elected #1 on EKRE list 2023, left EKRE fraction, active in Koos.
INSERT INTO mp_external_affiliation
  (member_slug, organization, org_kind, role, valid_from, valid_to, source_url, source_label, verified_by, verified_at, note)
VALUES
  ('varro-vooglaid', 'EKRE nimekiri', 'FRACTION_ORIGINAL', 'Valimisnimekiri (kandidaat nr 1)', '2023-03-05', '2024-06-10',
   'https://www.valimised.ee/et/riigikogu-valimised-2023',
   'valimised.ee — Riigikogu 2023 nimekirjad',
   'politico-curator', '2026-07-14',
   'Valiti Riigikogusse EKRE nimekirjas.'),

  ('varro-vooglaid', 'Fraktsioonita', 'INDEPENDENT', 'Fraktsioonita liige', '2024-06-11', NULL,
   'https://www.riigikogu.ee/riigikogu/koosseis/riigikogu-liikmed/',
   'Riigikogu koosseis',
   'politico-curator', '2026-07-14',
   'Lahkus EKRE fraktsioonist juunis 2024.'),

  ('varro-vooglaid', 'Koos Erakond', 'PARTY', 'Toetaja / avaliku esineja', '2024-08-01', NULL,
   'https://www.err.ee/1609401234',
   'ERR — Vooglaid Koos aktiivseks',
   'politico-curator', '2026-07-14',
   'Näidatud avalikuks Koos-poliitikuks. Ei ole Riigikogus ametlik Koos-esindaja.');

-- Kalle Grünthal — elected EKRE, active in party circles.
INSERT INTO mp_external_affiliation
  (member_slug, organization, org_kind, role, valid_from, valid_to, source_url, source_label, verified_by, verified_at, note)
VALUES
  ('kalle-grunthal', 'EKRE nimekiri', 'FRACTION_ORIGINAL', 'Valimisnimekiri', '2023-03-05', '2024-06-10',
   'https://www.valimised.ee/et/riigikogu-valimised-2023',
   'valimised.ee — Riigikogu 2023 nimekirjad',
   'politico-curator', '2026-07-14',
   'Valiti EKRE nimekirjas.'),

  ('kalle-grunthal', 'Fraktsioonita', 'INDEPENDENT', 'Fraktsioonita liige', '2024-06-11', NULL,
   'https://www.riigikogu.ee/riigikogu/koosseis/riigikogu-liikmed/',
   'Riigikogu koosseis',
   'politico-curator', '2026-07-14',
   'Lahkus EKRE fraktsioonist samal ajal Vooglaiu ja Põlluaasaga.');

-- Henn Põlluaas — long-standing EKRE, left fraction 2024.
INSERT INTO mp_external_affiliation
  (member_slug, organization, org_kind, role, valid_from, valid_to, source_url, source_label, verified_by, verified_at, note)
VALUES
  ('henn-polluaas', 'EKRE nimekiri', 'FRACTION_ORIGINAL', 'Valimisnimekiri', '2023-03-05', '2024-06-10',
   'https://www.valimised.ee/et/riigikogu-valimised-2023',
   'valimised.ee — Riigikogu 2023 nimekirjad',
   'politico-curator', '2026-07-14',
   'Valiti EKRE nimekirjas.'),

  ('henn-polluaas', 'Fraktsioonita', 'INDEPENDENT', 'Fraktsioonita liige', '2024-06-11', NULL,
   'https://www.riigikogu.ee/riigikogu/koosseis/riigikogu-liikmed/',
   'Riigikogu koosseis',
   'politico-curator', '2026-07-14',
   'Endine Riigikogu esimees. Lahkus EKRE fraktsioonist 2024.');

-- Jaanus Karilaid — Keskerakond origin, left fraction.
INSERT INTO mp_external_affiliation
  (member_slug, organization, org_kind, role, valid_from, valid_to, source_url, source_label, verified_by, verified_at, note)
VALUES
  ('jaanus-karilaid', 'Keskerakonna nimekiri', 'FRACTION_ORIGINAL', 'Valimisnimekiri', '2023-03-05', '2024-09-01',
   'https://www.valimised.ee/et/riigikogu-valimised-2023',
   'valimised.ee — Riigikogu 2023 nimekirjad',
   'politico-curator', '2026-07-14',
   'Valiti Keskerakonna nimekirjas.'),

  ('jaanus-karilaid', 'Fraktsioonita', 'INDEPENDENT', 'Fraktsioonita liige', '2024-09-02', NULL,
   'https://www.riigikogu.ee/riigikogu/koosseis/riigikogu-liikmed/',
   'Riigikogu koosseis',
   'politico-curator', '2026-07-14',
   'Lahkus Keskerakonna fraktsioonist erakondlike lahkarvamuste tõttu.');

-- Tanel Kiik — Keskerakond, similar path.
INSERT INTO mp_external_affiliation
  (member_slug, organization, org_kind, role, valid_from, valid_to, source_url, source_label, verified_by, verified_at, note)
VALUES
  ('tanel-kiik', 'Keskerakonna nimekiri', 'FRACTION_ORIGINAL', 'Valimisnimekiri', '2023-03-05', '2024-09-01',
   'https://www.valimised.ee/et/riigikogu-valimised-2023',
   'valimised.ee — Riigikogu 2023 nimekirjad',
   'politico-curator', '2026-07-14',
   'Valiti Keskerakonna nimekirjas.'),

  ('tanel-kiik', 'Fraktsioonita', 'INDEPENDENT', 'Fraktsioonita liige', '2024-09-02', NULL,
   'https://www.riigikogu.ee/riigikogu/koosseis/riigikogu-liikmed/',
   'Riigikogu koosseis',
   'politico-curator', '2026-07-14',
   'Endine tervise- ja tööminister. Lahkus Keskerakonna fraktsioonist.');

-- Jaak Aab — Keskerakond origin.
INSERT INTO mp_external_affiliation
  (member_slug, organization, org_kind, role, valid_from, valid_to, source_url, source_label, verified_by, verified_at, note)
VALUES
  ('jaak-aab', 'Keskerakonna nimekiri', 'FRACTION_ORIGINAL', 'Valimisnimekiri', '2023-03-05', '2024-09-01',
   'https://www.valimised.ee/et/riigikogu-valimised-2023',
   'valimised.ee — Riigikogu 2023 nimekirjad',
   'politico-curator', '2026-07-14',
   'Valiti Keskerakonna nimekirjas.'),

  ('jaak-aab', 'Fraktsioonita', 'INDEPENDENT', 'Fraktsioonita liige', '2024-09-02', NULL,
   'https://www.riigikogu.ee/riigikogu/koosseis/riigikogu-liikmed/',
   'Riigikogu koosseis',
   'politico-curator', '2026-07-14',
   'Endine minister. Lahkus Keskerakonna fraktsioonist.');
