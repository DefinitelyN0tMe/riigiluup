-- Link adopted acts to their Riigi Teataja publication. rt_act_id is RT's numeric act id,
-- which deterministically encodes the official citation (110032022001 = RT I, 10.03.2022, 1)
-- and builds both the publication URL (/akt/{id}) and the consolidated-text URL
-- (/akt/{id}?leiaKehtiv). Matched by exact publication date + title via RT's search API;
-- ambiguous matches stay NULL rather than guessing.

ALTER TABLE legislative_item ADD COLUMN rt_act_id BIGINT;
ALTER TABLE legislative_item ADD COLUMN rt_published DATE;
