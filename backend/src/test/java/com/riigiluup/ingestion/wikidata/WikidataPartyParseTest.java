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

    /** Distinct keys survive; null-start rows never collide (Postgres NULLs are distinct). */
    @Test
    void keepsDistinctKeysAndAllNullStartRows() {
        var a = new WikidataImporter.PartyRow("Q1", "Q10", "A", LocalDate.of(2000, 1, 1), null);
        var b = new WikidataImporter.PartyRow("Q1", "Q11", "B", LocalDate.of(2000, 1, 1), null);
        var n1 = new WikidataImporter.PartyRow("Q1", "Q10", "A", null, null);
        var n2 = new WikidataImporter.PartyRow("Q1", "Q10", "A", null, null);

        List<WikidataImporter.PartyRow> out =
                WikidataImporter.dedupeForUniqueKey(List.of(a, b, n1, n2));

        assertThat(out).hasSize(4);
    }
}
