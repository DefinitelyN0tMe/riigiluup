package com.riigiluup.api;

import com.riigiluup.initiative.Initiative;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Plain-HTML fact summaries for search and AI crawlers that do not run JavaScript (OAI-SearchBot,
 * PerplexityBot, ClaudeBot and others). Without this they received an empty SPA shell: no name, no
 * figures, nothing to cite. Every figure here comes from the SAME DTO the public API returns to the
 * page (the controllers are reused, not re-implemented), so a crawler reads exactly what a human sees,
 * only as static text. Estonian first (the site's main language), then short English and Russian
 * summaries for readers who ask an assistant in those languages. Pure functions, no I/O.
 *
 * <p>The block is rendered inside {@code <div id="root">}; the SPA's createRoot().render() replaces it,
 * so a browser that runs JavaScript never shows it twice.
 */
final class CrawlerContent {

    private CrawlerContent() {}

    static final String SITE = "https://riigiluup.ee";
    private static final ZoneId TALLINN = ZoneId.of("Europe/Tallinn");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final Map<String, String[]> READING = Map.of(
            "INITIATION", new String[]{"Algatamine", "Initiation", "Внесение"},
            "FIRST_READING", new String[]{"Esimene lugemine", "First reading", "Первое чтение"},
            "SECOND_READING", new String[]{"Teine lugemine", "Second reading", "Второе чтение"},
            "THIRD_READING", new String[]{"Kolmas lugemine", "Third reading", "Третье чтение"});

    private static final Map<String, String> DRAFT_TYPE_ET = Map.of(
            "SE", "seaduse eelnõu",
            "OE", "Riigikogu otsuse eelnõu");

    /** A rendered page: the fact block for #root, a meta description, and a JSON-LD object. */
    record Page(String title, String description, String bodyHtml, String jsonLd) {}

    // ---------------------------------------------------------------- MP

    static Page politician(PoliticianProfileDto p) {
        String name = p.fullName();
        String faction = p.faction() != null ? p.faction().name() : null;
        String role = p.active() ? "Riigikogu liige" : "Endine Riigikogu liige";
        String url = SITE + "/politicians/" + p.slug();

        StringBuilder b = new StringBuilder();
        b.append("<article lang=\"et\"><h1>").append(esc(name)).append("</h1>");
        b.append("<p>").append(esc(role));
        if (faction != null) b.append(". Fraktsioon: ").append(esc(faction));
        if (p.electoralDistrict() != null) b.append(". Valimisringkond: ").append(esc(p.electoralDistrict()));
        b.append(".</p><ul>");
        List<String> descBits = new ArrayList<>();
        if (p.voting() != null && p.voting().totalVotings() > 0) {
            String pct = pct(p.voting().participationRate(), Locale.forLanguageTag("et"));
            b.append(li("Hääletustel osalemine: " + pct + " (" + p.voting().participated() + " / "
                    + p.voting().totalVotings() + " registreeritud hääletusest)"));
            descBits.add("hääletustel osalemine " + pct);
        }
        if (p.participation() != null && p.participation().totalSittings() > 0) {
            b.append(li("Kohalolek istungitel: " + pct(p.participation().participationRate(), Locale.forLanguageTag("et"))
                    + " (" + p.participation().attended() + " / " + p.participation().totalSittings() + " istungist)"));
        }
        if (p.attendanceChecks() != null && p.attendanceChecks().totalSittings() > 0) {
            b.append(li("Kohaloleku kontrollil kohal: " + pct(p.attendanceChecks().participationRate(), Locale.forLanguageTag("et"))
                    + " (" + p.attendanceChecks().attended() + " / " + p.attendanceChecks().totalSittings() + ")"));
        }
        if (p.activity() != null) {
            var a = p.activity();
            b.append(li("Sõnavõtte täiskogus: " + a.speeches() + "; küsimusi: " + a.questions()
                    + "; arupärimisi: " + a.interpellations() + "; kirjalikke küsimusi: " + a.writtenQuestions()));
            descBits.add(a.speeches() + " sõnavõttu");
        }
        if (p.election() != null && p.election().personalVotes() > 0) {
            b.append(li("Riigikogu valimised 2023: " + p.election().personalVotes() + " isiklikku häält"
                    + (p.election().partyName() != null ? " (" + esc(p.election().partyName()) + " nimekiri)" : "")));
        }
        if (p.committees() != null && !p.committees().isEmpty()) {
            List<String> names = p.committees().stream().map(PoliticianProfileDto.GroupMembershipDto::name)
                    .filter(n -> n != null && !n.isBlank()).toList();
            if (!names.isEmpty()) b.append(li("Komisjonid: " + esc(String.join(", ", names))));
        }
        b.append("</ul>");
        b.append(sources(p.officialProfileUrl(), "Ametlik profiil Riigikogu veebis"));
        b.append("</article>");

        // Short English + Russian summaries: assistants are asked in these languages too.
        String votePctEn = p.voting() != null && p.voting().totalVotings() > 0
                ? pct(p.voting().participationRate(), Locale.ENGLISH) : null;
        String votePctRu = p.voting() != null && p.voting().totalVotings() > 0
                ? pct(p.voting().participationRate(), Locale.forLanguageTag("ru")) : null;
        b.append("<section lang=\"en\"><p>").append(esc(name)).append(" is ")
                .append(p.active() ? "a member" : "a former member").append(" of the Riigikogu, the parliament of Estonia")
                .append(faction != null ? " (" + esc(faction) + ")" : "").append('.');
        if (votePctEn != null) b.append(" Voted in ").append(votePctEn).append(" of recorded roll-call votes.");
        if (p.activity() != null) b.append(' ').append(p.activity().speeches()).append(" plenary speeches.");
        b.append("</p></section>");
        b.append("<section lang=\"ru\"><p>").append(esc(name)).append(p.active() ? " — депутат" : " — бывший депутат")
                .append(" Рийгикогу, парламента Эстонии")
                .append(faction != null ? " (" + esc(faction) + ")" : "").append('.');
        if (votePctRu != null) b.append(" Участие в поимённых голосованиях: ").append(votePctRu).append('.');
        if (p.activity() != null) b.append(" Выступлений на пленарных заседаниях: ").append(p.activity().speeches()).append('.');
        b.append("</p></section>");

        String desc = name + ": " + role.toLowerCase(Locale.ROOT)
                + (faction != null ? ", " + faction : "")
                + (descBits.isEmpty() ? "" : ". " + capitalize(String.join(", ", descBits)))
                + ". Andmed Riigikogu avaandmetest.";

        List<String> sameAs = new ArrayList<>();
        addIf(sameAs, p.officialProfileUrl());
        addIf(sameAs, p.wikipediaUrlEt());
        addIf(sameAs, p.wikipediaUrlEn());
        if (p.wikidataQid() != null && !p.wikidataQid().isBlank()) sameAs.add("https://www.wikidata.org/wiki/" + p.wikidataQid());
        StringBuilder ld = new StringBuilder("{\"@context\":\"https://schema.org\",\"@type\":\"Person\"");
        ld.append(",\"name\":").append(js(name)).append(",\"url\":").append(js(url));
        ld.append(",\"jobTitle\":").append(js(role));
        if (faction != null) ld.append(",\"memberOf\":{\"@type\":\"Organization\",\"name\":").append(js(faction)).append('}');
        if (!sameAs.isEmpty()) ld.append(",\"sameAs\":").append(jsArr(sameAs));
        ld.append('}');
        return new Page(name + " — Riigiluup", desc, wrap(b), ld.toString());
    }

    // ---------------------------------------------------------------- vote

    static Page vote(VoteDetailDto v) {
        String title = v.description() != null ? v.description() : "Nimeline hääletus";
        String when = v.startedAt() != null ? DATE_TIME.format(v.startedAt().atZone(TALLINN)) : null;
        StringBuilder b = new StringBuilder();
        b.append("<article lang=\"et\"><h1>").append(esc(title)).append("</h1><p>Nimeline hääletus Riigikogu täiskogus");
        if (when != null) b.append(", ").append(when);
        if (v.sittingTitle() != null) b.append(" (").append(esc(v.sittingTitle())).append(')');
        b.append(".</p><ul>");
        b.append(li("Poolt: " + v.resultInFavor() + "; vastu: " + v.resultAgainst() + "; erapooletuid: "
                + v.resultAbstained() + "; puudus: " + v.resultAbsent()));
        if (v.linkedBill() != null && v.linkedBill().title() != null) {
            b.append("<li>Eelnõu: <a href=\"").append(esc(SITE + "/legislation/" + v.linkedBill().id())).append("\">")
                    .append(esc(v.linkedBill().title())).append("</a></li>");
        }
        b.append("</ul>");
        if (v.factionBreakdowns() != null && !v.factionBreakdowns().isEmpty()) {
            b.append("<h2>Fraktsioonide kaupa</h2><ul>");
            for (var f : v.factionBreakdowns()) {
                b.append(li(esc(f.factionName()) + ": poolt " + f.inFavor() + ", vastu " + f.against()
                        + ", erapooletu " + f.abstained()));
            }
            b.append("</ul>");
        }
        b.append(sources(v.sourceUrl(), "Hääletuse andmed Riigikogu avaandmetes"));
        b.append("</article>");
        b.append("<section lang=\"en\"><p>Roll-call vote in the Estonian parliament (Riigikogu)")
                .append(when != null ? " on " + when : "").append(": ").append(v.resultInFavor()).append(" for, ")
                .append(v.resultAgainst()).append(" against, ").append(v.resultAbstained()).append(" abstained.</p></section>");
        b.append("<section lang=\"ru\"><p>Поимённое голосование в Рийгикогу")
                .append(when != null ? " " + when : "").append(": за ").append(v.resultInFavor()).append(", против ")
                .append(v.resultAgainst()).append(", воздержались ").append(v.resultAbstained()).append(".</p></section>");
        String desc = title + (when != null ? " (" + when + ")" : "") + ": poolt " + v.resultInFavor()
                + ", vastu " + v.resultAgainst() + ", erapooletuid " + v.resultAbstained() + ".";
        return new Page(title + " — Riigiluup", desc, wrap(b), null);
    }

    // ---------------------------------------------------------------- bill

    static Page legislation(LegislationDetailDto d) {
        String title = d.title() != null ? d.title() : "Eelnõu";
        String mark = d.mark() != null ? d.mark() + (d.draftTypeCode() != null ? " " + d.draftTypeCode() : "") : null;
        String url = SITE + "/legislation/" + d.id();
        StringBuilder b = new StringBuilder();
        b.append("<article lang=\"et\"><h1>").append(esc(title)).append("</h1><p>");
        String type = d.draftTypeCode() != null ? DRAFT_TYPE_ET.get(d.draftTypeCode()) : null;
        b.append(esc(type != null ? capitalize(type) : "Eelnõu"));
        if (mark != null) b.append(' ').append(esc(mark));
        b.append(" Riigikogu menetluses");
        if (d.initiatedDate() != null) b.append(", algatatud ").append(DATE.format(d.initiatedDate()));
        b.append(".</p><ul>");
        if (d.sponsors() != null && !d.sponsors().isEmpty()) {
            List<String> s = d.sponsors().stream().map(LegislationDetailDto.SponsorDto::displayName)
                    .filter(n -> n != null && !n.isBlank()).distinct().toList();
            if (!s.isEmpty()) b.append(li("Algatajad: " + esc(String.join(", ", s))));
        }
        if (d.leadingCommitteeName() != null) b.append(li("Juhtivkomisjon: " + esc(d.leadingCommitteeName())));
        if (d.acceptedDate() != null) b.append(li("Vastu võetud: " + DATE.format(d.acceptedDate())));
        if (d.rtPublished() != null) b.append(li("Avaldatud Riigi Teatajas: " + DATE.format(d.rtPublished())));
        b.append("</ul>");
        if (d.stages() != null && !d.stages().isEmpty()) {
            b.append("<h2>Menetluse käik</h2><ul>");
            for (var st : d.stages()) {
                String[] l = READING.get(st.readingCode());
                String label = l != null ? l[0] : st.readingCode();
                if (label == null) continue;
                b.append(li(esc(label) + (st.occurredAt() != null ? ": " + DATE.format(st.occurredAt().atZone(TALLINN)) : "")));
            }
            b.append("</ul>");
        }
        if (d.votes() != null && !d.votes().isEmpty()) {
            b.append("<h2>Hääletused</h2><ul>");
            for (var v : d.votes()) {
                b.append("<li><a href=\"").append(esc(SITE + "/votes/" + v.id())).append("\">")
                        .append(esc(v.description() != null ? v.description() : "Hääletus")).append("</a>")
                        .append(v.startedAt() != null ? " (" + DATE.format(v.startedAt().atZone(TALLINN)) + ")" : "")
                        .append(": poolt ").append(v.resultInFavor()).append(", vastu ").append(v.resultAgainst())
                        .append(", erapooletu ").append(v.resultAbstained()).append("</li>");
            }
            b.append("</ul>");
        }
        b.append(sources(d.riigikoguPageUrl(), "Eelnõu leht Riigikogu veebis"));
        b.append("</article>");
        b.append("<section lang=\"en\"><p>Bill").append(mark != null ? " " + esc(mark) : "")
                .append(" in the Estonian parliament (Riigikogu)")
                .append(d.initiatedDate() != null ? ", initiated " + DATE.format(d.initiatedDate()) : "")
                .append(": ").append(esc(title)).append(".</p></section>");
        b.append("<section lang=\"ru\"><p>Законопроект").append(mark != null ? " " + esc(mark) : "")
                .append(" в Рийгикогу")
                .append(d.initiatedDate() != null ? ", внесён " + DATE.format(d.initiatedDate()) : "")
                .append(": ").append(esc(title)).append(".</p></section>");

        String desc = (mark != null ? mark + ": " : "") + title
                + (d.initiatedDate() != null ? ". Algatatud " + DATE.format(d.initiatedDate()) : "")
                + ". Menetluse käik ja hääletused Riigikogu avaandmetest.";
        StringBuilder ld = new StringBuilder("{\"@context\":\"https://schema.org\",\"@type\":\"Legislation\"");
        ld.append(",\"name\":").append(js(title)).append(",\"url\":").append(js(url));
        if (mark != null) ld.append(",\"legislationIdentifier\":").append(js(mark));
        if (d.initiatedDate() != null) ld.append(",\"legislationDate\":").append(js(d.initiatedDate().toString()));
        ld.append(",\"legislationJurisdiction\":\"EE\"");
        ld.append(",\"legislationPassedBy\":{\"@type\":\"GovernmentOrganization\",\"name\":\"Riigikogu\"}");
        if (d.riigikoguPageUrl() != null) ld.append(",\"sameAs\":").append(js(d.riigikoguPageUrl()));
        ld.append('}');
        return new Page(title + " — Riigiluup", desc, wrap(b), ld.toString());
    }

    // ---------------------------------------------------------------- initiative

    static Page initiative(Initiative i) {
        String title = i.getTitle() != null ? i.getTitle() : "Kollektiivne pöördumine";
        String published = i.getPublishedAt() != null ? DATE.format(i.getPublishedAt().atZone(TALLINN)) : null;
        String source = i.getUuid() != null ? "https://rahvaalgatus.ee/initiatives/" + i.getUuid() : null;
        StringBuilder b = new StringBuilder();
        b.append("<article lang=\"et\"><h1>").append(esc(title)).append("</h1><p>Kollektiivne pöördumine")
                .append(published != null ? ", avaldatud " + published : "").append(".</p><ul>");
        if (i.getAuthors() != null && !i.getAuthors().isBlank()) b.append(li("Algatajad: " + esc(i.getAuthors())));
        if (i.getSignatureCount() != null) b.append(li("Allkirju: " + i.getSignatureCount()));
        if (i.getSentToParliamentAt() != null) {
            b.append(li("Riigikogule üle antud: " + DATE.format(i.getSentToParliamentAt().atZone(TALLINN))));
        }
        b.append("</ul>");
        b.append(sources(source, "Pöördumine rahvaalgatus.ee veebis"));
        b.append("</article>");
        b.append("<section lang=\"en\"><p>Citizen initiative in Estonia").append(published != null ? ", published " + published : "")
                .append(": ").append(esc(title)).append(i.getSignatureCount() != null ? ". Signatures: " + i.getSignatureCount() : "")
                .append(".</p></section>");
        b.append("<section lang=\"ru\"><p>Коллективное обращение").append(published != null ? ", опубликовано " + published : "")
                .append(": ").append(esc(title)).append(i.getSignatureCount() != null ? ". Подписей: " + i.getSignatureCount() : "")
                .append(".</p></section>");
        String desc = title + (i.getSignatureCount() != null ? ". Allkirju: " + i.getSignatureCount() : "")
                + ". Kollektiivne pöördumine Riigikogule.";
        return new Page(title + " — Riigiluup", desc, wrap(b), null);
    }

    // ---------------------------------------------------------------- helpers

    private static String wrap(StringBuilder body) {
        return "<main data-crawler-summary=\"riigiluup\">" + body
                + "<p><a href=\"" + SITE + "/\">Riigiluup</a>: Riigikogu avaandmed loetavaks. Faktid, mitte hinnangud. "
                + "<a href=\"" + SITE + "/methodology\">Metoodika</a></p></main>";
    }

    private static String sources(String url, String label) {
        if (url == null || url.isBlank()) return "";
        return "<p>Allikas: <a href=\"" + esc(url) + "\">" + esc(label) + "</a></p>";
    }

    private static String li(String alreadyEscapedOrPlain) {
        return "<li>" + alreadyEscapedOrPlain + "</li>";
    }

    static String pct(double rate, Locale locale) {
        String s = String.format(Locale.ROOT, "%.1f", rate * 100);
        if (!Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) s = s.replace('.', ',');
        return s + "%";
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void addIf(List<String> l, String v) {
        if (v != null && !v.isBlank()) l.add(v);
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** JSON string literal, also safe inside a script element (no "</" can close the tag). */
    static String js(String s) {
        StringBuilder o = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> o.append("\\\"");
                case '\\' -> o.append("\\\\");
                case '\n' -> o.append("\\n");
                case '\r' -> o.append("\\r");
                case '\t' -> o.append("\\t");
                case '<' -> o.append("\\u003c");
                case '>' -> o.append("\\u003e");
                case '&' -> o.append("\\u0026");
                default -> {
                    if (c < 0x20) o.append(String.format("\\u%04x", (int) c));
                    else o.append(c);
                }
            }
        }
        return o.append('"').toString();
    }

    private static String jsArr(List<String> items) {
        return "[" + String.join(",", items.stream().map(CrawlerContent::js).toList()) + "]";
    }
}
