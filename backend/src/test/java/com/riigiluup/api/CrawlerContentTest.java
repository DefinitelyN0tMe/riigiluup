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
}
