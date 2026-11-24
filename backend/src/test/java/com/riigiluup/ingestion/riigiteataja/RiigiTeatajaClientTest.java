package com.riigiluup.ingestion.riigiteataja;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiigiTeatajaClientTest {

    @Test
    void shortTitleIsKeptAsIs() {
        assertThat(RiigiTeatajaClient.searchText("  Karistusseadustiku muutmise seadus "))
                .isEqualTo("Karistusseadustiku muutmise seadus");
    }

    @Test
    void longTitleIsCutAtWordBoundaryWithinLimit() {
        String title = "Ühinenud Rahvaste Organisatsiooni mereõiguse konventsiooni alusel sõlmitava, riigi "
                + "jurisdiktsiooni alt välja jäävate merealade elurikkuse kaitse ja kestliku kasutamise "
                + "kokkuleppe ratifitseerimise seadus";
        String cut = RiigiTeatajaClient.searchText(title);
        assertThat(title.length()).isGreaterThan(RiigiTeatajaClient.MAX_SEARCH_CHARS);
        assertThat(cut.length()).isLessThanOrEqualTo(RiigiTeatajaClient.MAX_SEARCH_CHARS);
        assertThat(title).startsWith(cut);
        assertThat(Character.isWhitespace(title.charAt(cut.length()))).isTrue();
        assertThat(cut).endsWith("ratifitseerimise");
    }

    @Test
    void cutExactlyBeforeASpaceKeepsTheWholeLastWord() {
        String word = "a".repeat(RiigiTeatajaClient.MAX_SEARCH_CHARS);
        assertThat(RiigiTeatajaClient.searchText(word + " seadus")).isEqualTo(word);
    }
}
