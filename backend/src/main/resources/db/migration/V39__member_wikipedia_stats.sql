-- Wikipedia "reach" stats surfaced on the MP profile, suggested by Wikimedia Eesti:
--   wikipedia_lang_count       = number of language versions the person's Wikipedia article exists in
--                                (count of *.wikipedia.org sitelinks on the Wikidata item)
--   wikipedia_pageviews_90d    = total human pageviews of the person's primary-language Wikipedia
--                                article over the trailing ~90 days (Wikimedia REST pageviews API)
-- Both are populated by WikidataImporter for MPs already cross-referenced to a Wikidata Q-ID.
-- Nullable: unset when the person has no Wikipedia article or the stat could not be fetched.
ALTER TABLE plenary_member ADD COLUMN wikipedia_lang_count INTEGER;
ALTER TABLE plenary_member ADD COLUMN wikipedia_pageviews_90d INTEGER;
