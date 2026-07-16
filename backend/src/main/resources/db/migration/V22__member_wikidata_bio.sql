-- Wikidata bio enrichment for MPs: education institutions (P69) and other public
-- offices held (P39). Populated by the Wikidata cross-reference job; nullable.
ALTER TABLE plenary_member ADD COLUMN education TEXT;
ALTER TABLE plenary_member ADD COLUMN positions TEXT;
