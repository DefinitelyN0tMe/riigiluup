package com.riigiluup.api;

import com.riigiluup.initiative.Initiative;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlerContentTest {

    @Test
    void escapesHtmlSoSourceTextCanNeverInjectMarkup() {
        Initiative i = Initiative.builder()
                .title("Pealkiri <script>alert(1)</script> & \"jutumärgid\"")
                .authors("A <b>B</b>")
                .signatureCount(1234)
                .publishedAt(Instant.parse("2026-03-01T10:00:00Z"))
                .build();

        CrawlerContent.Page p = CrawlerContent.initiative(i);

        assertThat(p.bodyHtml()).doesNotContain("<script>").doesNotContain("<b>B</b>")
                .contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; &quot;jutumärgid&quot;")
                .contains("Allkirju: 1234")
                .contains("avaldatud 01.03.2026");
    }

    @Test
    void jsonStringsCannotCloseTheScriptElement() {
        String js = CrawlerContent.js("x</script><script>alert(1)</script>\"\\");
        assertThat(js).doesNotContain("</script>").doesNotContain("<").doesNotContain(">")
                .startsWith("\"").endsWith("\"")
                .contains("\\u003c/script\\u003e").contains("\\\"").contains("\\\\");
    }

    @Test
    void percentUsesTheReadersDecimalSeparator() {
        assertThat(CrawlerContent.pct(0.603358, Locale.forLanguageTag("et"))).isEqualTo("60,3%");
        assertThat(CrawlerContent.pct(0.603358, Locale.forLanguageTag("ru"))).isEqualTo("60,3%");
        assertThat(CrawlerContent.pct(0.603358, Locale.ENGLISH)).isEqualTo("60.3%");
    }

    @Test
    void initiativeWithoutOptionalFieldsStillRenders() {
        CrawlerContent.Page p = CrawlerContent.initiative(Initiative.builder().build());
        assertThat(p.title()).isEqualTo("Kollektiivne pöördumine — Riigiluup");
        assertThat(p.bodyHtml()).contains("<h1>Kollektiivne pöördumine</h1>").doesNotContain("null");
    }

    @Test
    void hubPagesLinkEveryEntityAndEscapeNames() {
        var mps = java.util.List.of(
                new CrawlerContent.MpLink("jaak-aab", "Jaak Aab", "Fraktsioon <x>"),
                new CrawlerContent.MpLink("a-b", "A \"B\" & C", null));
        CrawlerContent.Page p = CrawlerContent.politiciansList(mps);
        assertThat(p.bodyHtml())
                .contains("href=\"https://riigiluup.ee/politicians/jaak-aab\"")
                .contains("href=\"https://riigiluup.ee/politicians/a-b\"")
                .contains("(Fraktsioon &lt;x&gt;)")
                .contains("A &quot;B&quot; &amp; C")
                .contains("2 saadikut");

        var bill = new CrawlerContent.BillLink(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                903, "SE", "Ravimiseaduse <muutmise> eelnõu", java.time.LocalDate.of(2026, 9, 15));
        CrawlerContent.Page home = CrawlerContent.home(
                java.util.List.of(new CrawlerContent.NamedCount("Eesti Reformierakonna fraktsioon", 35)),
                101, java.util.List.of(bill), java.util.List.of());
        assertThat(home.bodyHtml())
                .contains("Praeguses koosseisus on 101 saadikut")
                .contains("903 SE: Ravimiseaduse &lt;muutmise&gt; eelnõu")
                .contains("/legislation/00000000-0000-0000-0000-000000000001")
                .contains("(15.09.2026)")
                .doesNotContain("<muutmise>");
    }
}
