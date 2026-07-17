package com.riigiluup.ingestion.rahvaalgatus;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RahvaalgatusCsvParserTest {

    /**
     * The source embeds real line breaks INSIDE quoted fields — the live file has 1292
     * physical lines for 1141 records. A naive line split corrupts the data, so this is the
     * single most important behaviour of the parser.
     */
    @Test
    void readsMultiLineTitleAsOneRecord() {
        String csv = """
                id,title,phase
                1,"Pöördumine
                teisel real",done
                2,Lihtne pealkiri,sign
                """;

        List<RahvaalgatusCsvParser.Row> rows = RahvaalgatusCsvParser.parse(csv);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).title()).isEqualTo("Pöördumine\nteisel real");
        assertThat(rows.get(1).title()).isEqualTo("Lihtne pealkiri");
    }

    /** Several committees arrive newline-separated inside one quoted field. */
    @Test
    void splitsMultipleCommitteesOnNewline() {
        String csv = """
                id,parliament_committees
                1,"social-affairs
                environment"
                2,finance
                3,
                """;

        List<RahvaalgatusCsvParser.Row> rows = RahvaalgatusCsvParser.parse(csv);

        assertThat(rows.get(0).committees()).containsExactly("social-affairs", "environment");
        assertThat(rows.get(1).committees()).containsExactly("finance");
        assertThat(rows.get(2).committees()).isEmpty();
    }

    @Test
    void parsesTimestampsAndLeavesBlanksNull() {
        String csv = """
                id,published_at,sent_to_parliament_at
                1,2026-06-09T09:24:16.381Z,
                """;

        RahvaalgatusCsvParser.Row row = RahvaalgatusCsvParser.parse(csv).get(0);

        assertThat(row.publishedAt()).isEqualTo(Instant.parse("2026-06-09T09:24:16.381Z"));
        assertThat(row.sentToParliamentAt()).isNull();
    }

    /**
     * 8 of the 201 initiatives actually sent to parliament have an empty signature_count —
     * blank must stay null, never become 0, or the threshold funnel lies.
     */
    @Test
    void blankSignatureCountIsNullNotZero() {
        String csv = """
                id,signature_count
                1,1500
                2,
                """;

        List<RahvaalgatusCsvParser.Row> rows = RahvaalgatusCsvParser.parse(csv);

        assertThat(rows.get(0).signatureCount()).isEqualTo(1500);
        assertThat(rows.get(1).signatureCount()).isNull();
    }

    /** New source columns must not break the import. */
    @Test
    void toleratesUnknownAndMissingColumns() {
        String csv = """
                id,brand_new_column,title
                1,whatever,Pealkiri
                """;

        RahvaalgatusCsvParser.Row row = RahvaalgatusCsvParser.parse(csv).get(0);

        assertThat(row.externalId()).isEqualTo("1");
        assertThat(row.title()).isEqualTo("Pealkiri");
        assertThat(row.destination()).isNull();
    }

    @Test
    void emptyInputYieldsNoRows() {
        assertThat(RahvaalgatusCsvParser.parse("id,title\n")).isEmpty();
    }
}
