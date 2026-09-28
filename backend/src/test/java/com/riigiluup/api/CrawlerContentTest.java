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

    @Test
    void analyticsMirrorsPageRoundingAndSurvivesMissingSections() {
        var latency = new com.riigiluup.analytics.AnalyticsDto.ResponseLatencyBoard(java.util.List.of(
                new com.riigiluup.analytics.AnalyticsDto.ResponseLatencyItem("Kristen Michal", "peaminister Kristen Michal",
                        153, 128, 44, 47.0, 8),
                new com.riigiluup.analytics.AnalyticsDto.ResponseLatencyItem("A <b>", null, 10, 0, 0, null, 0)),
                java.time.LocalDate.of(2023, 4, 10), Instant.now());
        var agreement = new com.riigiluup.analytics.AnalyticsDto.FactionAgreementMatrix(
                java.util.List.of(
                        new com.riigiluup.analytics.AnalyticsDto.FactionCell("1", "F1", "Reform", "x", 36),
                        new com.riigiluup.analytics.AnalyticsDto.FactionCell("2", "F2", "EKRE", "x", 9),
                        new com.riigiluup.analytics.AnalyticsDto.FactionCell("3", "F3", "Uus", "x", 1)),
                java.util.List.of(java.util.List.of(1.0, 0.1994, 0.5), java.util.Arrays.asList(0.1994, 1.0, null),
                        java.util.Arrays.asList(0.5, null, 1.0)),
                java.util.List.of(java.util.List.of(10, 1389, 5), java.util.List.of(1389, 10, 5), java.util.List.of(5, 0, 5)),
                1638, Instant.now());

        CrawlerContent.Page p = CrawlerContent.analytics(new CrawlerContent.AnalyticsData(
                latency, agreement, null, null, null, null, null, null, null, null));

        assertThat(p.title()).isEqualTo("Analüütika — Riigiluup");
        assertThat(p.bodyHtml())
                .contains("Kristen Michal (peaminister): 153 küsimust, 34% tähtaegselt, vastuse mediaan 47 päeva, üle tähtaja praegu: 8")
                .contains("kokku 163 küsimust ja arupärimist, neist 128 on vastatud, 44 tähtaegselt (34% vastatutest)")
                .contains("alates 10.04.2023")
                .contains("A &lt;b&gt;: 10 küsimust</li>")
                .contains("Reform ja EKRE: 20% (1389 hääletust)")
                .doesNotContain("EKRE ja Uus")
                .doesNotContain("<b>")
                .doesNotContain("null")
                .doesNotContain("XI. Kõige aktiivsemad");
    }

    private static LegislationDetailDto bill(String phase, String stage, java.time.LocalDate accepted) {
        return new LegislationDetailDto(java.util.UUID.fromString("00000000-0000-0000-0000-000000000127"), "x",
                127, 15, "SE", "Ravimiseaduse muutmise seaduse eelnõu", null, phase, stage, null, null,
                java.time.LocalDate.of(2023, 10, 25), java.time.LocalDate.of(2023, 6, 1), accepted, null, null, null,
                java.util.List.of(new LegislationDetailDto.StageDto("EFFECTUATION", null, null, 1)),
                java.util.List.of(), java.util.List.of(), null, "https://www.riigikogu.ee/tegevus/eelnoud/eelnou/x",
                null, null, java.util.List.of(), java.util.List.of());
    }

    @Test
    void billStatusReflectsTheRealOutcomeNotAlwaysInProceedings() {
        CrawlerContent.Page rejected = CrawlerContent.legislation(bill("REJECTED", "TAGASI_LYKATUD", null));
        assertThat(rejected.bodyHtml())
                .contains("Seaduse eelnõu 127 SE (XV Riigikogu), algatatud 01.06.2023. Tagasi lükatud 25.10.2023.")
                .contains("Status: rejected").contains("Статус: отклонён")
                .doesNotContain("menetluses").doesNotContain("EFFECTUATION").contains("Jõustumine");
        assertThat(rejected.jsonLd()).doesNotContain("legislationPassedBy");

        CrawlerContent.Page adopted = CrawlerContent.legislation(
                bill("ADOPTED", "VASTU_VOETUD", java.time.LocalDate.of(2024, 2, 14)));
        assertThat(adopted.bodyHtml()).contains("Vastu võetud 14.02.2024.");
        assertThat(adopted.jsonLd()).contains("legislationPassedBy");

        assertThat(CrawlerContent.legislation(bill("IN_READINGS", "TEINE_LUGEMINE", null)).bodyHtml())
                .contains("Riigikogu menetluses (teine lugemine).");
        assertThat(CrawlerContent.legislation(bill("OTHER", "VALJA_LANGENUD_KOOSEISU_LOPPEMISEGA", null)).bodyHtml())
                .contains("Menetlusest välja langenud Riigikogu koosseisu volituste lõppemisega 25.10.2023.");
    }

    @Test
    void attendanceCheckIsNotRenderedAsAVote() {
        var v = new VoteDetailDto(java.util.UUID.randomUUID(), "x", 104293, "ATTENDANCE_CHECK", null,
                "Kohaloleku kontroll", null, "Täiskogu istung", Instant.parse("2026-09-28T12:00:00Z"), null,
                0, 0, 101, 0, 91, 10, null, java.util.List.of(), java.util.List.of(),
                "https://api.riigikogu.ee/api/votings/x", "https://www.riigikogu.ee/x");
        CrawlerContent.Page p = CrawlerContent.vote(v);
        assertThat(p.bodyHtml())
                .contains("Kohaloleku kontroll Riigikogu täiskogus, 28.09.2026 15:00")
                .contains("Kohal: 91; puudus: 10")
                .contains("91 present, 10 absent")
                .contains("href=\"https://www.riigikogu.ee/x\"")
                .doesNotContain("ei hääletanud").doesNotContain("Nimeline hääletus");
        assertThat(p.title()).isEqualTo("Kohaloleku kontroll 28.09.2026 — Riigiluup");
    }
}
