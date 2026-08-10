package com.riigiluup.election;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalElectionImporterTest {

    @Test
    void splitsPlainRow() {
        String[] f = HistoricalElectionImporter.splitCsv("1975-06-22,reinsalu,Urmas Reinsalu,2003,Nomme,Isamaa,2754,1");
        assertThat(f).containsExactly("1975-06-22", "reinsalu", "Urmas Reinsalu", "2003", "Nomme", "Isamaa", "2754", "1");
    }

    @Test
    void keepsCommasInsideQuotedFields() {
        // district_name and party may contain commas; a quoted field must stay whole.
        String[] f = HistoricalElectionImporter.splitCsv(
                "1979-10-11,jufereva|skuratovski,Maria Jufereva-Skuratovski,2019,\"Kesklinn, Lasnamäe, Pirita\",\"Keskerakond\",1011,1");
        assertThat(f).hasSize(8);
        assertThat(f[4]).isEqualTo("Kesklinn, Lasnamäe, Pirita");
        assertThat(f[5]).isEqualTo("Keskerakond");
        assertThat(f[7]).isEqualTo("1");
    }

    @Test
    void handlesEscapedQuote() {
        String[] f = HistoricalElectionImporter.splitCsv("a,\"say \"\"hi\"\"\",b");
        assertThat(f).containsExactly("a", "say \"hi\"", "b");
    }
}
