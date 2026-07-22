package com.riigiluup.ingestion.wikidata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WikidataPartyParseTest {

    private static final ObjectMapper M = new ObjectMapper();

    private static JsonNode bindings(String json) throws Exception {
        return M.readTree(json);
    }

    /** et label preferred, dates from ISO instants, one row per statement. */
    @Test
    void parsesDatedMembership_preferringEstonianLabel() throws Exception {
        JsonNode b = bindings("""
            [
              {"person":{"value":"http://www.wikidata.org/entity/Q100"},
               "party":{"value":"http://www.wikidata.org/entity/Q200"},
               "partyLabelEt":{"value":"Reformierakond"},
               "partyLabelEn":{"value":"Reform Party"},
               "start":{"value":"2015-06-01T00:00:00Z"},
               "end":{"value":"2020-01-10T00:00:00Z"}}
            ]
            """);

        List<WikidataImporter.PartyRow> rows = WikidataImporter.parsePartyRows(b);

        assertThat(rows).hasSize(1);
        WikidataImporter.PartyRow r = rows.get(0);
        assertThat(r.personQid()).isEqualTo("Q100");
        assertThat(r.partyQid()).isEqualTo("Q200");
        assertThat(r.label()).isEqualTo("Reformierakond");
        assertThat(r.startDate()).isEqualTo(LocalDate.of(2015, 6, 1));
        assertThat(r.endDate()).isEqualTo(LocalDate.of(2020, 1, 10));
    }

    /** No et label → en fallback; missing dates → null; missing party skipped. */
    @Test
    void fallsBackToEnglish_andToleratesMissingDatesAndParty() throws Exception {
        JsonNode b = bindings("""
            [
              {"person":{"value":"http://www.wikidata.org/entity/Q1"},
               "party":{"value":"http://www.wikidata.org/entity/Q2"},
               "partyLabelEn":{"value":"Some Party"}},
              {"person":{"value":"http://www.wikidata.org/entity/Q1"}}
            ]
            """);

        List<WikidataImporter.PartyRow> rows = WikidataImporter.parsePartyRows(b);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).label()).isEqualTo("Some Party");
        assertThat(rows.get(0).startDate()).isNull();
        assertThat(rows.get(0).endDate()).isNull();
    }

    /** No label at all → fall back to the QID so the row is never blank. */
    @Test
    void fallsBackToQidWhenNoLabel() throws Exception {
        JsonNode b = bindings("""
            [
              {"person":{"value":"http://www.wikidata.org/entity/Q1"},
               "party":{"value":"http://www.wikidata.org/entity/Q9"}}
            ]
            """);

        List<WikidataImporter.PartyRow> rows = WikidataImporter.parsePartyRows(b);

        assertThat(rows.get(0).label()).isEqualTo("Q9");
    }

    /** Two claims collapsing to the same (person, party, start) — the crash from the ALL seed —
     *  collapse to one, keeping the row that carries an end date. */
    @Test
    void dedupesSamePersonPartyStart_preferringRowWithEndDate() {
        var noEnd = new WikidataImporter.PartyRow("Q1", "Q79854", "Isamaa",
                LocalDate.of(1986, 1, 1), null);
        var withEnd = new WikidataImporter.PartyRow("Q1", "Q79854", "Isamaa",
                LocalDate.of(1986, 1, 1), LocalDate.of(1995, 1, 1));

        List<WikidataImporter.PartyRow> out =
                WikidataImporter.dedupeForUniqueKey(List.of(noEnd, withEnd));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).endDate()).isEqualTo(LocalDate.of(1995, 1, 1));
    }

    /** A malformed party QID (non-Q / over-long) is rejected: the whole P102 statement is dropped
     *  so it can never reach the VARCHAR(32) party_qid column. */
    @Test
    void skipsStatementsWithMalformedQid() throws Exception {
        JsonNode b = bindings("""
            [
              {"person":{"value":"http://www.wikidata.org/entity/Q1"},
               "party":{"value":"http://www.wikidata.org/entity/not-a-qid"},
               "partyLabelEt":{"value":"Bogus"}},
              {"person":{"value":"http://www.wikidata.org/entity/Q1"},
               "party":{"value":"http://www.wikidata.org/entity/Q200"},
               "partyLabelEt":{"value":"Reformierakond"}}
            ]
            """);

        List<WikidataImporter.PartyRow> rows = WikidataImporter.parsePartyRows(b);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).partyQid()).isEqualTo("Q200");
    }

    /** An over-long label (world-editable free text) is truncated to the party_label column width. */
    @Test
    void truncatesOversizedLabel() throws Exception {
        String huge = "x".repeat(5000);
        JsonNode b = bindings("""
            [
              {"person":{"value":"http://www.wikidata.org/entity/Q1"},
               "party":{"value":"http://www.wikidata.org/entity/Q2"},
               "partyLabelEt":{"value":"%s"}}
            ]
            """.formatted(huge));

        List<WikidataImporter.PartyRow> rows = WikidataImporter.parsePartyRows(b);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).label()).hasSize(256);
    }

    /** QID format guard. */
    @Test
    void validatesQidFormat() {
        assertThat(WikidataImporter.isValidQid("Q42")).isTrue();
        assertThat(WikidataImporter.isValidQid("Q0")).isTrue();
        assertThat(WikidataImporter.isValidQid("q42")).isFalse();
        assertThat(WikidataImporter.isValidQid("Q42x")).isFalse();
        assertThat(WikidataImporter.isValidQid("42")).isFalse();
        assertThat(WikidataImporter.isValidQid("Q")).isFalse();
        assertThat(WikidataImporter.isValidQid(null)).isFalse();
    }

    /** Only https *.wikipedia.org URLs are accepted; javascript:/http/off-domain are rejected so
     *  they can never be persisted and later rendered as an href. */
    @Test
    void acceptsOnlyHttpsWikipediaUrls() {
        assertThat(WikidataImporter.isWikipediaUrl("https://et.wikipedia.org/wiki/Foo")).isTrue();
        assertThat(WikidataImporter.isWikipediaUrl("https://wikipedia.org/wiki/Foo")).isTrue();
        assertThat(WikidataImporter.isWikipediaUrl("http://et.wikipedia.org/wiki/Foo")).isFalse();
        assertThat(WikidataImporter.isWikipediaUrl("javascript:alert(1)")).isFalse();
        assertThat(WikidataImporter.isWikipediaUrl("https://evil.example.com/wiki")).isFalse();
        assertThat(WikidataImporter.isWikipediaUrl("https://et.wikipedia.org.evil.com/x")).isFalse();
        assertThat(WikidataImporter.isWikipediaUrl("not a url")).isFalse();
        assertThat(WikidataImporter.isWikipediaUrl(null)).isFalse();
        // Bypass attempts: userinfo host-spoof, no-dot prefix, scheme case, backslash trick.
        assertThat(WikidataImporter.isWikipediaUrl("https://et.wikipedia.org@evil.com/x")).isFalse();
        assertThat(WikidataImporter.isWikipediaUrl("https://xxwikipedia.org/x")).isFalse();
        assertThat(WikidataImporter.isWikipediaUrl("https://evil.com\\@et.wikipedia.org")).isFalse();
        // Legitimate URLs with mixed case and non-ASCII path still pass.
        assertThat(WikidataImporter.isWikipediaUrl("HTTPS://ET.WIKIPEDIA.ORG/wiki/Foo")).isTrue();
        assertThat(WikidataImporter.isWikipediaUrl("https://et.wikipedia.org/wiki/Jüri_Ratas")).isTrue();
    }

    /** clamp leaves short values untouched and only shortens over-long ones. */
    @Test
    void clampLeavesShortValuesUntouched() {
        assertThat(WikidataImporter.clamp("f", "Q1", "short", 256)).isEqualTo("short");
        assertThat(WikidataImporter.clamp("f", "Q1", null, 256)).isNull();
        assertThat(WikidataImporter.clamp("f", "Q1", "abcdef", 3)).isEqualTo("abc");
    }

    /** Distinct keys survive; null-start duplicates collapse too — the V29 constraint is
     *  NULLS NOT DISTINCT, so they would collide in the DB. */
    @Test
    void keepsDistinctKeysAndCollapsesNullStartDuplicates() {
        var a = new WikidataImporter.PartyRow("Q1", "Q10", "A", LocalDate.of(2000, 1, 1), null);
        var b = new WikidataImporter.PartyRow("Q1", "Q11", "B", LocalDate.of(2000, 1, 1), null);
        var n1 = new WikidataImporter.PartyRow("Q1", "Q10", "A", null, null);
        var n2 = new WikidataImporter.PartyRow("Q1", "Q10", "A", null, null);

        List<WikidataImporter.PartyRow> out =
                WikidataImporter.dedupeForUniqueKey(List.of(a, b, n1, n2));

        assertThat(out).hasSize(3);
        assertThat(out.stream().filter(r -> r.startDate() == null)).hasSize(1);
    }
}
