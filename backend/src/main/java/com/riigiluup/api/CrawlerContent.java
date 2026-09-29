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

    private static final Map<String, String[]> READING = Map.ofEntries(
            Map.entry("INITIATION", new String[]{"Algatamine", "Initiation", "Внесение"}),
            Map.entry("FIRST_READING", new String[]{"Esimene lugemine", "First reading", "Первое чтение"}),
            Map.entry("SECOND_READING", new String[]{"Teine lugemine", "Second reading", "Второе чтение"}),
            Map.entry("THIRD_READING", new String[]{"Kolmas lugemine", "Third reading", "Третье чтение"}),
            Map.entry("ESIMENE_LUGEMINE", new String[]{"Esimene lugemine", "First reading", "Первое чтение"}),
            Map.entry("TEINE_LUGEMINE", new String[]{"Teine lugemine", "Second reading", "Второе чтение"}),
            Map.entry("KOLMAS_LUGEMINE", new String[]{"Kolmas lugemine", "Third reading", "Третье чтение"}),
            Map.entry("EFFECTUATION", new String[]{"Jõustumine", "Entry into force", "Вступление в силу"}),
            Map.entry("VASTU_VOETUD", new String[]{"Vastu võetud", "Adopted", "Принят"}),
            Map.entry("LOPETATUD", new String[]{"Lõpetatud", "Closed", "Завершено"}));

    /** Step statuses as the bill page names them (frontend i18n stageStatus.*). */
    private static final Map<String, String> STAGE_STATUS_ET = Map.of(
            "ALGATATUD", "algatatud", "MENETLUSSE_VOETUD", "menetlusse võetud", "LOPETATUD", "lõpetatud",
            "SAADETUD_VABARIIGI_PRESIDENDILE", "saadetud Vabariigi Presidendile", "VALJAKUULUTATUD", "välja kuulutatud",
            "AVALDATUD_RIIGITEATAJAS", "avaldatud Riigi Teatajas", "TAGASI_LYKATUD", "tagasi lükatud",
            "TAGASI_VOETUD", "tagasi võetud");

    private static final Map<String, String> DRAFT_TYPE_ET = Map.of(
            "SE", "seaduse eelnõu",
            "OE", "Riigikogu otsuse eelnõu");

    /** A rendered page: the fact block for #root, a meta description, and a JSON-LD object. */
    record Page(String title, String description, String bodyHtml, String jsonLd) {}

    // ---------------------------------------------------------------- MP

    static Page politician(PoliticianProfileDto p) {
        String name = p.fullName();
        String faction = p.faction() != null ? p.faction().name() : null;
        boolean nonAttached = faction != null && faction.toLowerCase(Locale.ROOT).contains("mittekuuluv");
        String role = p.active() ? "Riigikogu liige" : "Endine Riigikogu liige";
        String url = SITE + "/politicians/" + p.slug();

        StringBuilder b = new StringBuilder();
        b.append("<article lang=\"et\"><h1>").append(esc(name)).append("</h1>");
        b.append("<p>").append(esc(role));
        if (nonAttached) b.append(". Ei kuulu ühtegi fraktsiooni");
        else if (faction != null) b.append(". Fraktsioon: ").append(esc(faction));
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
            b.append(li("Sõnavõtte täiskogu stenogrammis: " + a.speeches() + "; küsimusi: " + a.questions()
                    + "; arupärimisi: " + a.interpellations() + "; kirjalikke küsimusi: " + a.writtenQuestions()));
            descBits.add(a.speeches() + " sõnavõttu stenogrammis");
        }
        if (p.election() != null && p.election().personalVotes() > 0) {
            b.append(li("Riigikogu valimised 2023: " + p.election().personalVotes() + " isiklikku häält"
                    + (p.election().partyName() != null ? " (nimekiri: " + esc(p.election().partyName()) + ")" : "")));
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
                .append(nonAttached ? ", not affiliated with any parliamentary group"
                        : faction != null ? " (" + esc(faction) + ")" : "").append('.');
        if (votePctEn != null) b.append(" Voted in ").append(votePctEn).append(" of recorded roll-call votes.");
        if (p.activity() != null) b.append(' ').append(p.activity().speeches()).append(" plenary speeches.");
        b.append("</p></section>");
        b.append("<section lang=\"ru\"><p>").append(esc(name)).append(p.active() ? " — депутат" : " — бывший депутат")
                .append(" Рийгикогу, парламента Эстонии")
                .append(nonAttached ? ", не входит ни в одну фракцию"
                        : faction != null ? " (" + esc(faction) + ")" : "").append('.');
        if (votePctRu != null) b.append(" Участие в поимённых голосованиях: ").append(votePctRu).append('.');
        if (p.activity() != null) b.append(" Выступлений на пленарных заседаниях: ").append(p.activity().speeches()).append('.');
        b.append("</p></section>");

        String desc = name + ": " + role
                + (nonAttached ? ", fraktsioonidesse mittekuuluv" : faction != null ? ", " + faction : "")
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
        if (faction != null && !nonAttached) ld.append(",\"memberOf\":{\"@type\":\"Organization\",\"name\":").append(js(faction)).append('}');
        if (!sameAs.isEmpty()) ld.append(",\"sameAs\":").append(jsArr(sameAs));
        ld.append('}');
        return new Page(name + " — Riigiluup", desc, wrap(b), ld.toString());
    }

    // ---------------------------------------------------------------- vote

    static Page vote(VoteDetailDto v) {
        String motion = v.description() != null ? v.description() : "Nimeline hääletus";
        boolean attendance = "ATTENDANCE_CHECK".equals(v.type());
        String billTitle = v.linkedBill() != null ? v.linkedBill().title() : null;
        String billLabel = billTitle == null ? null
                : (v.linkedBill().mark() != null ? v.linkedBill().mark() + ": " : "") + billTitle;
        String when = v.startedAt() != null ? DATE_TIME.format(v.startedAt().atZone(TALLINN)) : null;
        String day = v.startedAt() != null ? DATE.format(v.startedAt().atZone(TALLINN)) : null;
        // Titles name the bill and the date, so thousands of "Lõpphääletus" pages are distinguishable.
        String title = (billLabel != null ? billLabel + ". " : "") + motion + (day != null ? " " + day : "");
        String sourceUrl = v.riigikoguPageUrl() != null ? v.riigikoguPageUrl() : v.sourceUrl();
        StringBuilder b = new StringBuilder();
        b.append("<article lang=\"et\"><h1>").append(esc(motion)).append("</h1><p>")
         .append(attendance ? "Kohaloleku kontroll Riigikogu täiskogus" : "Nimeline hääletus Riigikogu täiskogus");
        if (when != null) b.append(", ").append(when);
        if (v.sittingTitle() != null) b.append(" (").append(esc(v.sittingTitle())).append(')');
        b.append(".</p><ul>");
        // The source's resultAbstained overlaps (did-not-vote + absent); the true partition is the one
        // the vote page uses (frontend lib/voteTally): abstained = neutral, didNotVote = present minus
        // the three cast choices, absent = absent. It sums to the seat count. An attendance check is
        // not a vote: only who was in the hall.
        int abstained = v.resultNeutral();
        int didNotVote = Math.max(0, v.resultPresent() - v.resultInFavor() - v.resultAgainst() - v.resultNeutral());
        String tallyEt = attendance
                ? "Kohal: " + v.resultPresent() + "; puudus: " + v.resultAbsent()
                : "Poolt: " + v.resultInFavor() + "; vastu: " + v.resultAgainst() + "; erapooletuid: "
                        + abstained + "; ei hääletanud: " + didNotVote + "; puudus: " + v.resultAbsent();
        b.append(li(tallyEt));
        if (billTitle != null) {
            b.append("<li>Eelnõu: <a href=\"").append(esc(SITE + "/legislation/" + v.linkedBill().id())).append("\">")
                    .append(esc(billLabel)).append("</a></li>");
        }
        b.append("</ul>");
        if (!attendance && v.factionBreakdowns() != null && !v.factionBreakdowns().isEmpty()) {
            b.append("<h2>Fraktsioonide kaupa</h2><ul>");
            for (var f : v.factionBreakdowns()) {
                b.append(li(esc(f.factionName()) + ": poolt " + f.inFavor() + ", vastu " + f.against()
                        + ", erapooletu " + f.abstained()));
            }
            b.append("</ul>");
        }
        b.append(sources(sourceUrl, "Hääletus Riigikogu veebis"));
        b.append("</article>");
        if (attendance) {
            b.append("<section lang=\"en\"><p>Attendance check in the Estonian parliament (Riigikogu)")
             .append(when != null ? " on " + when : "").append(": ").append(v.resultPresent()).append(" present, ")
             .append(v.resultAbsent()).append(" absent.</p></section>");
            b.append("<section lang=\"ru\"><p>Проверка присутствия в Рийгикогу")
             .append(when != null ? " " + when : "").append(": присутствовали ").append(v.resultPresent())
             .append(", отсутствовали ").append(v.resultAbsent()).append(".</p></section>");
        } else {
            b.append("<section lang=\"en\"><p>Roll-call vote in the Estonian parliament (Riigikogu)")
             .append(when != null ? " on " + when : "").append(": ").append(v.resultInFavor()).append(" for, ")
             .append(v.resultAgainst()).append(" against, ").append(abstained).append(" abstained, ")
             .append(didNotVote).append(" did not vote, ").append(v.resultAbsent()).append(" absent.</p></section>");
            b.append("<section lang=\"ru\"><p>Поимённое голосование в Рийгикогу")
             .append(when != null ? " " + when : "").append(": за ").append(v.resultInFavor()).append(", против ")
             .append(v.resultAgainst()).append(", воздержались ").append(abstained).append(", не голосовали ")
             .append(didNotVote).append(", отсутствовали ").append(v.resultAbsent()).append(".</p></section>");
        }
        String desc = title + ": " + (attendance
                ? "kohal " + v.resultPresent() + ", puudus " + v.resultAbsent() + "."
                : "poolt " + v.resultInFavor() + ", vastu " + v.resultAgainst() + ", erapooletuid " + abstained
                        + ", ei hääletanud " + didNotVote + ", puudus " + v.resultAbsent() + ".");
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
        if (d.membership() != null) b.append(" (").append(roman(d.membership())).append(" Riigikogu)");
        if (d.initiatedDate() != null) b.append(", algatatud ").append(DATE.format(d.initiatedDate()));
        b.append(". ").append(esc(capitalize(statusEt(d)))).append(".</p><ul>");
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
                String status = st.statusCode() != null ? STAGE_STATUS_ET.get(st.statusCode()) : null;
                b.append(li(esc(label) + (status != null ? " (" + status + ")" : "")
                        + (st.occurredAt() != null ? ": " + DATE.format(st.occurredAt().atZone(TALLINN)) : "")));
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
                .append(": ").append(esc(title)).append(". Status: ").append(esc(statusEn(d))).append(".</p></section>");
        b.append("<section lang=\"ru\"><p>Законопроект").append(mark != null ? " " + esc(mark) : "")
                .append(" в Рийгикогу")
                .append(d.initiatedDate() != null ? ", внесён " + DATE.format(d.initiatedDate()) : "")
                .append(": ").append(esc(title)).append(". Статус: ").append(esc(statusRu(d))).append(".</p></section>");

        String desc = (mark != null ? mark + ": " : "") + title
                + (d.initiatedDate() != null ? ". Algatatud " + DATE.format(d.initiatedDate()) : "")
                + ". " + capitalize(statusEt(d)) + ". Menetluse käik ja hääletused Riigikogu avaandmetest.";
        StringBuilder ld = new StringBuilder("{\"@context\":\"https://schema.org\",\"@type\":\"Legislation\"");
        ld.append(",\"name\":").append(js(title)).append(",\"url\":").append(js(url));
        if (mark != null) ld.append(",\"legislationIdentifier\":").append(js(mark));
        if (d.initiatedDate() != null) ld.append(",\"legislationDate\":").append(js(d.initiatedDate().toString()));
        ld.append(",\"legislationJurisdiction\":\"EE\"");
        if ("ADOPTED".equals(d.phase())) {
            ld.append(",\"legislationPassedBy\":{\"@type\":\"GovernmentOrganization\",\"name\":\"Riigikogu\"}");
        }
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

    // ---------------------------------------------------------------- hub pages
    // Lists give a crawler what a human gets from the navigation: links to every current MP, the latest
    // bills and votes, committees and groups. Without them the entity pages were reachable only via the
    // sitemap, and the pages were an island with no internal links between them.

    /** Minimal views so the hubs do not depend on JPA entities or on several DTO shapes. */
    record MpLink(String slug, String name, String faction) {}
    record BillLink(java.util.UUID id, Integer mark, String typeCode, String title, java.time.LocalDate initiated) {}
    record VoteLink(java.util.UUID id, String description, java.time.Instant startedAt) {}
    record NamedCount(String name, long count) {}

    static Page home(List<NamedCount> factions, long activeMps, List<BillLink> bills, List<VoteLink> votes) {
        StringBuilder b = new StringBuilder("<article lang=\"et\"><h1>Riigiluup: Riigikogu avaandmed loetavaks</h1>");
        b.append("<p>Riigiluup on tasuta ja sõltumatu kodanikualgatus, mis koondab Eesti parlamendi Riigikogu avaandmed ")
         .append("ühte kohta: kuidas iga saadik hääletab, milliseid eelnõusid menetletakse, mida istungitel räägitakse ning ")
         .append("kui kiiresti ministrid saadikute küsimustele vastavad. Iga arv viitab ametlikule allikale. ")
         .append("Lehel ei ole hinnanguid ega pingeridu ning see ei ole seotud ühegi erakonnaga.</p>");
        b.append("<h2>Riigikogu koosseis</h2><p>Praeguses koosseisus on ").append(activeMps).append(" saadikut.</p><ul>");
        for (NamedCount f : factions) b.append(li(esc(f.name()) + ": " + f.count()));
        b.append("</ul><p><a href=\"").append(SITE).append("/politicians\">Kõik saadikud</a> · <a href=\"").append(SITE)
         .append("/legislation\">Eelnõud</a> · <a href=\"").append(SITE).append("/votes\">Hääletused</a> · <a href=\"")
         .append(SITE).append("/analytics\">Analüütika</a> · <a href=\"").append(SITE).append("/methodology\">Metoodika</a></p>");
        appendBills(b, "Viimati algatatud eelnõud", bills, 10);
        appendVotes(b, "Viimased hääletused", votes, 10);
        b.append("</article>");
        b.append("<section lang=\"en\"><p>Riigiluup is a free, independent citizen-built site that makes the open data of ")
         .append("Estonia's parliament, the Riigikogu, readable: how each MP votes, which bills are moving, what is said in ")
         .append("the chamber and how quickly ministers answer MPs' questions. Every figure links to its official source; ")
         .append("no ratings, no party affiliation. Available in Estonian, Russian and English.</p></section>");
        b.append("<section lang=\"ru\"><p>Riigiluup — бесплатный независимый гражданский сайт, который делает открытые данные ")
         .append("парламента Эстонии (Рийгикогу) понятными: как голосует каждый депутат, какие законопроекты рассматриваются, ")
         .append("что говорится на заседаниях и как быстро министры отвечают на вопросы депутатов. Каждая цифра ведёт к ")
         .append("официальному источнику; без оценок и без партийной принадлежности.</p></section>");
        return new Page("Riigiluup — Riigikogu läbipaistvus",
                "Riigikogu avaandmed loetavaks: kuidas iga saadik hääletab, mida ta algatab ja kus on kandideerinud. Faktid, mitte hinnangud.",
                wrap(b), null);
    }

    static Page politiciansList(List<MpLink> mps) {
        StringBuilder b = new StringBuilder("<article lang=\"et\"><h1>Riigikogu liikmed</h1><p>Praeguse Riigikogu koosseisu ")
                .append(mps.size()).append(" saadikut. Iga profiil näitab hääletusi, kohalolekut, sõnavõtte, algatatud eelnõusid, ")
                .append("komisjone ja valimistulemusi koos viidetega Riigikogu andmetele.</p><ul>");
        for (MpLink m : mps) {
            b.append("<li><a href=\"").append(esc(SITE + "/politicians/" + m.slug())).append("\">").append(esc(m.name()))
             .append("</a>").append(m.faction() != null ? " (" + esc(m.faction()) + ")" : "").append("</li>");
        }
        b.append("</ul></article><section lang=\"en\"><p>All ").append(mps.size())
         .append(" current members of the Riigikogu, the parliament of Estonia, each linking to a profile with votes, ")
         .append("attendance, speeches, bills and election results.</p></section><section lang=\"ru\"><p>Все ")
         .append(mps.size()).append(" действующих депутатов Рийгикогу со ссылками на профили.</p></section>");
        return new Page("Saadikud — Riigiluup", "Riigikogu praeguse koosseisu " + mps.size()
                + " saadikut: hääletused, kohalolek, sõnavõtud, eelnõud ja valimistulemused.", wrap(b), null);
    }

    static Page billsList(List<BillLink> bills) {
        StringBuilder b = new StringBuilder("<article lang=\"et\"><h1>Eelnõud</h1><p>Riigikogu menetluses olevad ja menetletud ")
                .append("eelnõud: algatajad, menetluse käik, hääletused ja avaldamine Riigi Teatajas.</p>");
        appendBills(b, "Viimati algatatud", bills, bills.size());
        b.append("</article><section lang=\"en\"><p>Bills in the Estonian parliament (Riigikogu): sponsors, readings, votes and ")
         .append("publication in the State Gazette.</p></section><section lang=\"ru\"><p>Законопроекты Рийгикогу: ")
         .append("инициаторы, чтения, голосования и публикация в Riigi Teataja.</p></section>");
        return new Page("Eelnõud — Riigiluup", "Riigikogu eelnõud: algatajad, menetluse käik ja hääletused.", wrap(b), null);
    }

    static Page votesList(List<VoteLink> votes) {
        StringBuilder b = new StringBuilder("<article lang=\"et\"><h1>Hääletused</h1><p>Riigikogu täiskogu nimelised hääletused: ")
                .append("tulemus ja iga saadiku valik, fraktsioonide kaupa.</p>");
        appendVotes(b, "Viimased hääletused", votes, votes.size());
        b.append("</article><section lang=\"en\"><p>Roll-call votes in the Estonian parliament (Riigikogu), with each MP's choice.</p>")
         .append("</section><section lang=\"ru\"><p>Поимённые голосования Рийгикогу с выбором каждого депутата.</p></section>");
        return new Page("Hääletused — Riigiluup", "Riigikogu nimelised hääletused: tulemus ja iga saadiku valik.", wrap(b), null);
    }

    static Page committee(com.riigiluup.committee.CommitteeDto.Detail c) {
        StringBuilder b = new StringBuilder("<article lang=\"et\"><h1>").append(esc(c.name())).append("</h1><p>Riigikogu komisjon")
                .append(c.members() != null ? ", " + c.members().size() + " liiget" : "").append(".</p>");
        if (c.members() != null && !c.members().isEmpty()) {
            b.append("<h2>Liikmed</h2><ul>");
            for (var m : c.members()) {
                String role = roleEt(m.role());
                b.append("<li>").append(m.slug() != null
                        ? "<a href=\"" + esc(SITE + "/politicians/" + m.slug()) + "\">" + esc(m.name()) + "</a>" : esc(m.name()))
                 .append(role != null ? ", " + role : "").append(m.factionName() != null ? " (" + esc(m.factionName()) + ")" : "")
                 .append("</li>");
            }
            b.append("</ul>");
        }
        if (c.ledBills() != null && c.ledBills().recent() != null && !c.ledBills().recent().isEmpty()) {
            b.append("<h2>Juhtivkomisjonina menetletud eelnõud (kokku ").append(c.ledBills().total()).append(")</h2><ul>");
            for (var r : c.ledBills().recent()) {
                b.append("<li><a href=\"").append(esc(SITE + "/legislation/" + r.id())).append("\">")
                 .append(r.mark() != null ? r.mark() + ": " : "").append(esc(r.title())).append("</a></li>");
            }
            b.append("</ul>");
        }
        b.append("</article><section lang=\"en\"><p>").append(esc(c.name()))
         .append(", a committee of the Estonian parliament (Riigikogu): members and bills it leads.</p></section>");
        return new Page(c.name() + " — Riigiluup", c.name() + ": Riigikogu komisjoni liikmed ja juhitavad eelnõud.", wrap(b), null);
    }

    static Page group(com.riigiluup.group.GroupDirectoryDto.Detail g) {
        String kind = switch (g.category() == null ? "" : g.category()) {
            case "FRIENDSHIP" -> "Parlamendirühm (sõprusrühm)";
            case "DELEGATION" -> "Riigikogu delegatsioon";
            case "SUPPORT" -> "Toetusrühm";
            default -> "Riigikogu ühendus";
        };
        StringBuilder b = new StringBuilder("<article lang=\"et\"><h1>").append(esc(g.name())).append("</h1><p>").append(kind)
                .append(g.members() != null ? ", " + g.members().size() + " liiget" : "").append(".</p>");
        if (g.members() != null && !g.members().isEmpty()) {
            b.append("<ul>");
            for (var m : g.members()) {
                b.append("<li>").append(m.slug() != null
                        ? "<a href=\"" + esc(SITE + "/politicians/" + m.slug()) + "\">" + esc(m.name()) + "</a>" : esc(m.name()))
                 .append(m.factionName() != null ? " (" + esc(m.factionName()) + ")" : "").append("</li>");
            }
            b.append("</ul>");
        }
        b.append("</article>");
        return new Page(g.name() + " — Riigiluup", g.name() + ": " + kind.toLowerCase(Locale.ROOT) + ", liikmed.", wrap(b), null);
    }

    private static final Map<String, String> FLOW_NODE_ET = Map.ofEntries(
            Map.entry("initiated", "Algatatud"), Map.entry("in_committee", "Komisjonis"),
            Map.entry("first_reading", "I lugemine"), Map.entry("second_reading", "II lugemine"),
            Map.entry("third_reading", "III lugemine"), Map.entry("in_readings", "Lugemistel"),
            Map.entry("submitted", "Esitatud"), Map.entry("adopted", "Vastu võetud"),
            Map.entry("rejected", "Tagasi lükatud"), Map.entry("withdrawn", "Tagasi võetud"), Map.entry("other", "Muu"));
    private static final Map<String, String> FINANCE_ET = Map.of(
            "state", "riigitoetus", "donations", "annetused", "membership", "liikmemaksud", "loans", "laenud", "other", "muu");
    private static final Map<String, String> FUNNEL_ET = Map.of(
            "targeted", "Riigikogule suunatud", "signing", "Allkirjade kogumisel", "threshold", "Allkirjade lävend täitunud",
            "sent", "Riigikogusse saadetud", "decided", "Otsustatud",
            "draftAct", "Võeti menetlusse eelnõuna või riikliku küsimusena");
    private static final Map<String, String> DECISION_ET = Map.of(
            "return", "Tagastatud esitajale", "reject", "Tagasi lükatud", "solve-differently", "Lahendatud muul viisil",
            "forward", "Edastatud", "forward-to-government", "Edastatud Vabariigi Valitsusele",
            "draft-act-or-national-matter", "Eelnõu või riiklikult tähtis küsimus");
    private static final Map<String, String> MANDATE_ET = Map.of(
            "PERSONAL", "Isikumandaat", "DISTRICT", "Ringkonnamandaat", "COMPENSATION", "Kompensatsioonimandaat",
            "SUBSTITUTE", "Asendusliige");

    /** Everything the /analytics page shows as numbers, from the same endpoints and with the same defaults. */
    record AnalyticsData(
            com.riigiluup.analytics.AnalyticsDto.ResponseLatencyBoard latency,
            com.riigiluup.analytics.AnalyticsDto.FactionAgreementMatrix agreement,
            com.riigiluup.analytics.AnalyticsDto.DisciplineBreakers discipline,
            com.riigiluup.analytics.AnalyticsDto.BillFlow flow,
            com.riigiluup.analytics.AnalyticsDto.BillVelocity velocity,
            com.riigiluup.analytics.AnalyticsDto.NightVotes night,
            com.riigiluup.analytics.AnalyticsDto.MemberActivityBoard activity,
            com.riigiluup.analytics.AnalyticsDto.ElectionBoard elections,
            com.riigiluup.analytics.AnalyticsDto.PartyFinanceBoard finance,
            com.riigiluup.initiative.InitiativeDto.Funnel funnel) {}

    /**
     * Crawler text for /analytics. Mirrors the page: same section numbers, titles and notes, same
     * rounding (whole percent / whole days where the page rounds), same top-N cuts. Sections that are
     * purely visual on the page (attendance grid, timing heatmap, topics, scatter, co-sponsorship) are
     * only named, with a pointer to the interactive page. A section whose data is missing is skipped.
     */
    static Page analytics(AnalyticsData d) {
        StringBuilder b = new StringBuilder("<article lang=\"et\"><h1>Riigikogu analüütika</h1>")
                .append("<p>Analüüsid ainult Riigikogu avaandmete põhjal. Iga arv on jälgitav ametliku allikani. ")
                .append("Ei arvamusi ega hinnanguid: ainult see, mida hääletused, kohalolekud, eelnõud ja ")
                .append("dokumendiregister ise ütlevad. Interaktiivsed graafikud: <a href=\"").append(SITE)
                .append("/analytics\">riigiluup.ee/analytics</a>. Arvutuskäik: <a href=\"").append(SITE)
                .append("/methodology/analytics\">metoodika</a>.</p>");

        var lat = d.latency();
        if (lat != null && lat.ministers() != null && !lat.ministers().isEmpty()) {
            long total = 0, answered = 0, onTime = 0, overdue = 0;
            for (var m : lat.ministers()) {
                total += m.total(); answered += m.answered(); onTime += m.answeredOnTime(); overdue += m.overdueNow();
            }
            b.append("<section id=\"vastamise-kiirus\"><h2>XIV. Kes vastab tähtaegselt: ministrite vastamise kiirus</h2>")
             .append("<p>Jooksva koosseisu arupärimised ja kirjalikud küsimused")
             .append(lat.since() != null ? " (alates " + DATE.format(lat.since()) + ")" : "")
             .append(", mõõdetuna allika enda fikseeritud vastamistähtaja vastu: esitamisest registreeritud kirjaliku ")
             .append("vastuseni või täiskogu istungini, kus minister arupärimisele vastas. Alla viie küsimusega ministrid ")
             .append("on peidetud; esitajatele tagastatud küsimused on välja jäetud.</p>")
             .append("<p>Loetletud ministritele on esitatud kokku ").append(total).append(" küsimust ja arupärimist, neist ")
             .append(answered).append(" on vastatud, ").append(onTime).append(" tähtaegselt")
             .append(answered > 0 ? " (" + Math.round(onTime * 100.0 / answered) + "% vastatutest)" : "")
             .append(". Praegu on üle tähtaja vastuseta ").append(overdue).append(".</p><ol>");
            for (var m : lat.ministers()) {
                String office = m.addresseeRole() == null ? null : m.addresseeRole().replace(m.addresseeName(), "").trim();
                b.append("<li>").append(esc(m.addresseeName()))
                 .append(office != null && !office.isEmpty() ? " (" + esc(office) + ")" : "")
                 .append(": ").append(m.total()).append(" küsimust");
                if (m.answered() > 0) b.append(", ").append(Math.round(m.answeredOnTime() * 100.0 / m.answered())).append("% tähtaegselt");
                if (m.medianDaysToAnswer() != null) b.append(", vastuse mediaan ").append(Math.round(m.medianDaysToAnswer())).append(" päeva");
                if (m.overdueNow() > 0) b.append(", üle tähtaja praegu: ").append(m.overdueNow());
                b.append("</li>");
            }
            b.append("</ol></section>");
        }

        var ag = d.agreement();
        if (ag != null && ag.factions() != null && ag.matrix() != null) {
            b.append("<section><h2>I. Kes kellega hääletab: fraktsioonide kokkulangevus</h2>")
             .append("<p>Kui tihti kaks fraktsiooni jõudsid samale enamuse otsusele. Andmed vaid nimelistelt hääletustelt, ")
             .append("kus mõlemal fraktsioonil oli selge enamus (kokku ").append(ag.totalVotesConsidered())
             .append(" hääletust).</p><ul>");
            var f = ag.factions();
            for (int i = 0; i < f.size(); i++) {
                for (int j = i + 1; j < f.size(); j++) {
                    Double v = cell(ag.matrix(), i, j);
                    if (v == null) continue;
                    Integer n = ag.support() == null ? null : intCell(ag.support(), i, j);
                    b.append("<li>").append(esc(f.get(i).shortName())).append(" ja ").append(esc(f.get(j).shortName()))
                     .append(": ").append(Math.round(v * 100)).append("%")
                     .append(n != null ? " (" + n + " hääletust)" : "").append("</li>");
                }
            }
            b.append("</ul></section>");
        }

        var disc = d.discipline();
        if (disc != null && disc.items() != null && !disc.items().isEmpty()) {
            b.append("<section><h2>II. Kes hääletab erinevalt oma fraktsioonist</h2>")
             .append("<p>").append(disc.items().size()).append(" saadikut, kelle hääl erines kõige sagedamini nende fraktsiooni ")
             .append("selge enamuse valikust, järjestatud erinevate häälte osakaalu järgi. Arvesse lähevad nimelised hääletused, ")
             .append("kus fraktsioonil oli selge enamus; hääli ajast, mil saadik fraktsiooni ei kuulunud, ei arvestata. ")
             .append("Järjekord näitab ainult osakaalu suurust: erinev hääl ei ole iseenesest hea ega halb.</p><ol>");
            for (var it : disc.items()) {
                b.append("<li><a href=\"").append(esc(SITE + "/politicians/" + it.memberSlug())).append("\">")
                 .append(esc(it.memberName())).append("</a>")
                 .append(it.factionShortName() != null ? " (" + esc(it.factionShortName()) + ")" : "")
                 .append(": ").append(pct(it.deviationRate(), ET)).append(" (").append(it.deviations()).append(" hääletusel ")
                 .append(it.eligible()).append("-st fraktsiooni enamusest erinevalt)</li>");
            }
            b.append("</ol></section>");
        }

        var flow = d.flow();
        if (flow != null && flow.nodes() != null && !flow.nodes().isEmpty()) {
            b.append("<section><h2>III. Kus eelnõud peatuvad: eelnõude vool</h2><p>Kokku ").append(flow.totalBills())
             .append(" eelnõu. Mitu eelnõu on jõudnud igasse menetlusetappi:</p><ul>");
            for (var n : flow.nodes()) {
                if (n.count() <= 0) continue;
                b.append(li(esc(FLOW_NODE_ET.getOrDefault(n.id(), n.label())) + ": " + n.count()));
            }
            b.append("</ul></section>");
        }

        var vel = d.velocity();
        if (vel != null && vel.totalAdopted() > 0) {
            b.append("<section><h2>VII. Kui kiiresti eelnõud läbi lähevad</h2><p>Päevi eelnõu algatamisest vastuvõtmiseni, ")
             .append(vel.totalAdopted()).append(" vastu võetud eelnõu põhjal: mediaan ").append(vel.medianDays())
             .append(" päeva, 90. protsentiil ").append(vel.p90Days()).append(" päeva, kiireim ").append(vel.fastestDays())
             .append(", aeglaseim ").append(vel.slowestDays()).append(" päeva.</p>");
            if (vel.buckets() != null && !vel.buckets().isEmpty()) {
                b.append("<ul>");
                for (var bu : vel.buckets()) b.append(li(esc(bu.label()) + ": " + bu.count()));
                b.append("</ul>");
            }
            b.append("</section>");
        }

        var night = d.night();
        if (night != null && night.totalVotes() > 0) {
            b.append("<section><h2>X. Öised hääletused</h2><p>Riigikogu tavalised istungid toimuvad päeval. Kõigist ")
             .append(night.totalVotes()).append(" nimelisest hääletusest ").append(night.nightVotes())
             .append(" (").append(pct(night.nightRatio(), ET)).append(") toimus enne kella ")
             .append(night.windowStartHour()).append(":00 või pärast ").append(night.windowEndHour())
             .append(":00, neist ").append(night.lateNightVotes()).append(" kella 22:00 ja 06:00 vahel; nädalavahetusel ")
             .append(night.weekendVotes()).append(". See ei tähenda automaatselt, et midagi on valesti.</p>");
            if (night.items() != null && !night.items().isEmpty()) {
                b.append("<ul>");
                for (var it : night.items().subList(0, Math.min(10, night.items().size()))) {
                    b.append("<li><a href=\"").append(esc(SITE + "/votes/" + it.voteId())).append("\">")
                     .append(esc(it.description() != null ? it.description() : "Hääletus")).append("</a>");
                    if (it.linkedBillTitle() != null) b.append(": ").append(esc(it.linkedBillTitle()));
                    b.append(" (").append(esc(localStamp(it.startedAt()))).append(", poolt ").append(it.forCount())
                     .append(", vastu ").append(it.againstCount()).append(")</li>");
                }
                b.append("</ul>");
            }
            b.append("</section>");
        }

        var act = d.activity();
        if (act != null && act.items() != null && !act.items().isEmpty()) {
            var ranked = act.items().stream()
                    .sorted((x, y) -> Integer.compare(y.speeches(), x.speeches())).limit(15).toList();
            b.append("<section><h2>XI. Kõige aktiivsemad saadikud</h2><p>Sõnavõtud ja küsimused täiskogu stenogrammidest, ")
             .append("pluss arupärimised ja kirjalikud küsimused jooksval koosseisul. 15 enim sõna võtnud saadikut. ")
             .append("Arvud kajastavad aktiivsuse mahtu, mitte selle sisu.</p><ol>");
            for (var m : ranked) {
                b.append("<li><a href=\"").append(esc(SITE + "/politicians/" + m.memberSlug())).append("\">")
                 .append(esc(m.memberName())).append("</a>")
                 .append(m.factionShortName() != null ? " (" + esc(m.factionShortName()) + ")" : "")
                 .append(": ").append(m.speeches()).append(" sõnavõttu, ").append(m.questions()).append(" küsimust, ")
                 .append(m.interpellations()).append(" arupärimist, ").append(m.writtenQuestions())
                 .append(" kirjalikku küsimust</li>");
            }
            b.append("</ol></section>");
        }

        var el = d.elections();
        if (el != null && el.members() != null && !el.members().isEmpty()) {
            b.append("<section><h2>XII. Kellel on enim isiklikke hääli</h2><p>Istuvad saadikud 2023. aasta ")
             .append("Riigikogu valimistel saadud isiklike häälte järgi (15 esimest). Asendajaid, keda ise ei valitud, ei ")
             .append("näidata.</p><ol>");
            for (var m : el.members().subList(0, Math.min(15, el.members().size()))) {
                b.append("<li><a href=\"").append(esc(SITE + "/politicians/" + m.memberSlug())).append("\">")
                 .append(esc(m.memberName())).append("</a>")
                 .append(m.partyName() != null ? " (" + esc(m.partyName()) + ")" : "")
                 .append(": ").append(m.personalVotes()).append(" häält")
                 .append(m.mandateType() != null ? ", " + esc(MANDATE_ET.getOrDefault(m.mandateType(), m.mandateType()).toLowerCase(Locale.ROOT)) : "")
                 .append("</li>");
            }
            b.append("</ol>");
            if (el.mandates() != null && !el.mandates().isEmpty()) {
                b.append("<p>Mandaaditüübid: ");
                List<String> parts = new ArrayList<>();
                for (var mc : el.mandates()) parts.add(esc(MANDATE_ET.getOrDefault(mc.mandateType(), mc.mandateType())) + " " + mc.count());
                b.append(String.join(", ", parts)).append(".</p>");
            }
            b.append("</section>");
        }

        var fin = d.finance();
        if (fin != null && fin.parties() != null && !fin.parties().isEmpty()) {
            b.append("<section><h2>XIII. Kuidas parteid on rahastatud</h2><p>Parlamendierakondade deklareeritud tulud alates ")
             .append(fin.sinceYear()).append(". aastast allikate kaupa, Erakondade Rahastamise Järelevalve Komisjoni (ERJK) ")
             .append("registrist. Summad on aruandelised, eurodes.</p><ul>");
            for (var p : fin.parties()) {
                b.append("<li>").append(esc(p.partyName())).append(": kokku ").append(p.total()).append(" €");
                if (p.buckets() != null && !p.buckets().isEmpty()) {
                    List<String> parts = new ArrayList<>();
                    for (var bu : p.buckets()) if (bu.amount() > 0) parts.add(FINANCE_ET.getOrDefault(bu.key(), bu.key()) + " " + bu.amount() + " €");
                    b.append(" (").append(String.join(", ", parts)).append(")");
                }
                b.append("</li>");
            }
            b.append("</ul></section>");
        }

        var fu = d.funnel();
        if (fu != null && fu.steps() != null && !fu.steps().isEmpty()) {
            b.append("<section><h2>XV. Mis saab kollektiivsetest pöördumistest</h2><p>Iga Rahvaalgatus.ee kaudu ")
             .append("Riigikogule esitatud kollektiivne pöördumine, esimesest kavandist lõpliku otsuseni.</p><ul>");
            for (var st : fu.steps()) b.append(li(esc(FUNNEL_ET.getOrDefault(st.key(), st.key())) + ": " + st.count()));
            b.append("</ul>");
            if (fu.decisions() != null && !fu.decisions().isEmpty()) {
                b.append("<p>Otsused: ");
                List<String> parts = new ArrayList<>();
                for (var dc : fu.decisions()) parts.add(esc(DECISION_ET.getOrDefault(dc.decision(), dc.decision())) + " " + dc.count());
                b.append(String.join(", ", parts)).append(".</p>");
            }
            if (fu.reachedThresholdButNeverSent() > 0) {
                b.append("<p>").append(fu.reachedThresholdButNeverSent())
                 .append(" pöördumist kogus nõutud 1000 allkirja, kuid neid Riigikogule kunagi ei saadetud.</p>");
            }
            if (fu.sentBelowThreshold() > 0) {
                b.append("<p>").append(fu.sentBelowThreshold())
                 .append(" pöördumist saadeti Riigikogusse, ehkki andmete järgi allkirjade lävendit ei täitunud.</p>");
            }
            if (fu.medianDaysToDecision() != null) {
                b.append("<p>Mediaan otsuseni: ").append(Math.round(fu.medianDaysToDecision())).append(" päeva (n = ")
                 .append(fu.medianSampleSize()).append(").</p>");
            }
            b.append("</section>");
        }

        b.append("<p>Ainult interaktiivsena lehel: IV kohalolek istungite kaupa, V hääletuste aeg nädalapäeva ja tunni ")
         .append("järgi, VI eelnõude teemad, VIII saadikute paiknemine hääletuste põhjal, IX koos algatamise võrgustik.</p>");
        b.append("</article>");

        b.append("<section lang=\"en\"><p>Analytics on the Estonian parliament (Riigikogu), computed only from its open data: ")
         .append("how often parliamentary groups vote the same way, MPs who most often vote against their group's majority, ")
         .append("where bills stop, how fast bills pass, night votes, the most active MPs, personal votes in the 2023 election, ")
         .append("party finance (ERJK) and citizen initiatives.");
        if (lat != null && lat.ministers() != null && !lat.ministers().isEmpty()) {
            b.append(" Section XIV measures how quickly each minister answers MPs' interpellations and written questions ")
             .append("against the deadline recorded by the source: share answered on time, median days to answer and ")
             .append("questions overdue right now.");
        }
        b.append("</p></section>");
        b.append("<section lang=\"ru\"><p>Аналитика по парламенту Эстонии (Рийгикогу) только на основе открытых данных: ")
         .append("насколько часто фракции голосуют одинаково, кто чаще других голосует против большинства своей фракции, ")
         .append("где останавливаются законопроекты, ночные голосования, самые активные депутаты, личные голоса на выборах 2023 года, ")
         .append("финансирование партий и гражданские инициативы. Раздел XIV: как быстро министры отвечают на запросы и ")
         .append("письменные вопросы депутатов относительно срока, указанного в источнике.</p></section>");

        return new Page("Analüütika — Riigiluup",
                "Riigikogu analüütika avaandmete põhjal: fraktsioonide kokkulangevus, ministrite vastamise kiirus, "
                        + "eelnõude vool, öised hääletused, aktiivsus, valimised ja erakondade rahastamine.",
                wrap(b), null);
    }

    private static final Locale ET = Locale.forLanguageTag("et");

    private static Double cell(List<List<Double>> m, int i, int j) {
        if (i >= m.size() || m.get(i) == null || j >= m.get(i).size()) return null;
        return m.get(i).get(j);
    }

    private static Integer intCell(List<List<Integer>> m, int i, int j) {
        if (i >= m.size() || m.get(i) == null || j >= m.get(i).size()) return null;
        return m.get(i).get(j);
    }

    /** "2026-06-17T21:10:51.860Z" -> "18.06.2026 00:10" (Tallinn); anything unparseable is shown as is. */
    private static String localStamp(String iso) {
        if (iso == null) return "";
        try {
            return DATE_TIME.format(java.time.Instant.parse(iso).atZone(TALLINN));
        } catch (Exception e) {
            try {
                return DATE_TIME.format(java.time.OffsetDateTime.parse(iso).atZoneSameInstant(TALLINN));
            } catch (Exception e2) {
                return iso;
            }
        }
    }

    private static String roleEt(String role) {
        if (role == null) return null;
        return switch (role) {
            case "CHAIR" -> "esimees";
            case "VICE_CHAIR" -> "aseesimees";
            default -> null;
        };
    }

    private static void appendBills(StringBuilder b, String heading, List<BillLink> bills, int max) {
        if (bills == null || bills.isEmpty()) return;
        b.append("<h2>").append(esc(heading)).append("</h2><ul>");
        for (BillLink x : bills.subList(0, Math.min(max, bills.size()))) {
            b.append("<li><a href=\"").append(esc(SITE + "/legislation/" + x.id())).append("\">")
             .append(x.mark() != null ? x.mark() + (x.typeCode() != null ? " " + esc(x.typeCode()) : "") + ": " : "")
             .append(esc(x.title())).append("</a>")
             .append(x.initiated() != null ? " (" + DATE.format(x.initiated()) + ")" : "").append("</li>");
        }
        b.append("</ul>");
    }

    private static void appendVotes(StringBuilder b, String heading, List<VoteLink> votes, int max) {
        if (votes == null || votes.isEmpty()) return;
        b.append("<h2>").append(esc(heading)).append("</h2><ul>");
        for (VoteLink v : votes.subList(0, Math.min(max, votes.size()))) {
            b.append("<li><a href=\"").append(esc(SITE + "/votes/" + v.id())).append("\">")
             .append(esc(v.description() != null ? v.description() : "Hääletus")).append("</a>")
             .append(v.startedAt() != null ? " (" + DATE_TIME.format(v.startedAt().atZone(TALLINN)) + ")" : "").append("</li>");
        }
        b.append("</ul>");
    }

    /** Where the bill stands, in the words the bill page uses; never "in proceedings" for a closed bill. */
    static String statusEt(LegislationDetailDto d) {
        String code = d.activeStageSourceCode() == null ? "" : d.activeStageSourceCode();
        // No date for non-adopted outcomes: the source's activeStatusDate is not the date of the
        // outcome (bill 985 OE: 02.09 while it was rejected on 23.09). The dated stages follow below.
        switch (d.phase() == null ? "OTHER" : d.phase()) {
            case "ADOPTED":
                return "vastu võetud" + (d.acceptedDate() != null ? " " + DATE.format(d.acceptedDate()) : "");
            case "REJECTED":
                return "tagasi lükatud";
            case "WITHDRAWN":
                return switch (code) {
                    case "TAGASTATUD" -> "tagastatud algatajale";
                    case "LOPETATUD" -> "menetlus lõpetatud";
                    default -> "tagasi võetud";
                };
            case "SUBMITTED":
                return "Riigikogu menetluses";
            case "IN_COMMITTEE":
                return "Riigikogu menetluses (komisjonis)";
            case "IN_READINGS": {
                if ("UUESTI_ARUTAMINE".equals(code)) return "Riigikogu menetluses (uuesti arutamisel)";
                String[] r = READING.get(code);
                return "Riigikogu menetluses" + (r != null ? " (" + r[0].toLowerCase(Locale.ROOT) + ")" : "");
            }
            default:
                return switch (code) {
                    case "VALJA_LANGENUD" -> "menetlusest välja langenud";
                    case "VALJA_LANGENUD_KOOSEISU_LOPPEMISEGA" -> "menetlusest välja langenud Riigikogu koosseisu volituste lõppemisega";
                    case "YHENDATUD" -> "ühendatud teise eelnõuga";
                    case "VALJA_ARVATUD" -> "menetlusest välja arvatud";
                    case "VALJA_KUULUTAMATA_JAETUD" -> "välja kuulutamata jäetud";
                    default -> "menetluse seis: vaata Riigikogu eelnõu lehte";
                };
        }
    }

    static String statusEn(LegislationDetailDto d) {
        return switch (d.phase() == null ? "OTHER" : d.phase()) {
            case "ADOPTED" -> "adopted" + (d.acceptedDate() != null ? " " + DATE.format(d.acceptedDate()) : "");
            case "REJECTED" -> "rejected";
            case "WITHDRAWN" -> "withdrawn or closed";
            case "SUBMITTED", "IN_COMMITTEE", "IN_READINGS" -> "in proceedings in the Riigikogu";
            default -> "no longer in proceedings (lapsed, merged or not promulgated; see the Riigikogu page)";
        };
    }

    static String statusRu(LegislationDetailDto d) {
        return switch (d.phase() == null ? "OTHER" : d.phase()) {
            case "ADOPTED" -> "принят" + (d.acceptedDate() != null ? " " + DATE.format(d.acceptedDate()) : "");
            case "REJECTED" -> "отклонён";
            case "WITHDRAWN" -> "отозван или производство прекращено";
            case "SUBMITTED", "IN_COMMITTEE", "IN_READINGS" -> "в производстве Рийгикогу";
            default -> "производство не ведётся (выбыл, объединён или не провозглашён; см. страницу Рийгикогу)";
        };
    }

    /** Riigikogu composition number as the parliament writes it (15 -> XV). */
    static String roman(int n) {
        int[] v = {10, 9, 5, 4, 1};
        String[] r = {"X", "IX", "V", "IV", "I"};
        StringBuilder o = new StringBuilder();
        for (int i = 0; i < v.length; i++) while (n >= v[i]) { o.append(r[i]); n -= v[i]; }
        return o.toString();
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
