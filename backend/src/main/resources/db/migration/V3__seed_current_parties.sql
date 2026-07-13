-- Deterministic party UUIDs so the seed is idempotent across environments.
INSERT INTO party (id, short_name, full_name, color_hex, official_url, active) VALUES
  ('11111111-1111-1111-1111-111111111101', 'RE',
   'Eesti Reformierakond', '#F9C812', 'https://reform.ee/', TRUE),
  ('11111111-1111-1111-1111-111111111102', 'KE',
   'Eesti Keskerakond', '#008C3D', 'https://keskerakond.ee/', TRUE),
  ('11111111-1111-1111-1111-111111111103', 'EKRE',
   'Eesti Konservatiivne Rahvaerakond', '#0F55A6', 'https://www.ekre.ee/', TRUE),
  ('11111111-1111-1111-1111-111111111104', 'E200',
   'Erakond Eesti 200', '#00B7C7', 'https://www.eesti200.ee/', TRUE),
  ('11111111-1111-1111-1111-111111111105', 'SDE',
   'Sotsiaaldemokraatlik Erakond', '#E30613', 'https://sotsdem.ee/', TRUE),
  ('11111111-1111-1111-1111-111111111106', 'I',
   'Isamaa Erakond', '#005AA5', 'https://isamaa.ee/', TRUE);

-- Fraction → party links (validity opens at start of 14th Riigikogu term, no close).
-- UUIDs harvested from live Riigikogu API on 2026-07-13.
INSERT INTO faction_party_link
  (id, faction_external_id, party_id, valid_from, valid_to) VALUES
  (gen_random_uuid(), '8772fd6f-3197-6a53-2ffc-8c4d63407d1e', '11111111-1111-1111-1111-111111111101', '2023-04-10', NULL),
  (gen_random_uuid(), '3c1832c0-7727-18d1-d9d3-e685a58f44b0', '11111111-1111-1111-1111-111111111102', '2023-04-10', NULL),
  (gen_random_uuid(), 'd4e90963-1d10-4f8a-bf37-a99ca8531ff3', '11111111-1111-1111-1111-111111111103', '2023-04-10', NULL),
  (gen_random_uuid(), 'e4bf6970-f928-4230-961c-615cc54118f9', '11111111-1111-1111-1111-111111111104', '2023-04-10', NULL),
  (gen_random_uuid(), 'd188e268-5d01-7e93-0c22-3ae7f0c1e851', '11111111-1111-1111-1111-111111111105', '2023-04-10', NULL),
  (gen_random_uuid(), 'a844d128-287d-4c20-bf30-61fcb0af23cf', '11111111-1111-1111-1111-111111111106', '2023-04-10', NULL);
