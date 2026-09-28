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
            b.append(li("Sõnavõtte täiskogus: " + a.speeches() + "; küsimusi: " + a.questions()
                    + "; arupärimisi: " + a.interpellations() + "; kirjalikke küsimusi: " + a.writtenQuestions()));
            descBits.add(a.speeches() + " sõnavõttu");
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
        String title = v.description() != null ? v.description() : "Nimeline hääletus";
        String when = v.startedAt() != null ? DATE_TIME.format(v.startedAt().atZone(TALLINN)) : null;
        StringBuilder b = new StringBuilder();
        b.append("<article lang=\"et\"><h1>").append(esc(title)).append("</h1><p>Nimeline hääletus Riigikogu täiskogus");
        if (when != null) b.append(", ").append(when);
        if (v.sittingTitle() != null) b.append(" (").append(esc(v.sittingTitle())).append(')');
        b.append(".</p><ul>");
        // The source's resultAbstained overlaps (did-not-vote + absent); the true partition is the one
        // the vote page uses (frontend lib/voteTally): abstained = neutral, didNotVote = present minus
        // the three cast choices, absent = absent. It sums to the seat count.
        int abstained = v.resultNeutral();
        int didNotVote = Math.max(0, v.resultPresent() - v.resultInFavor() - v.resultAgainst() - v.resultNeutral());
        b.append(li("Poolt: " + v.resultInFavor() + "; vastu: " + v.resultAgainst() + "; erapooletuid: "
                + abstained + "; ei hääletanud: " + didNotVote + "; puudus: " + v.resultAbsent()));
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
                .append(v.resultAgainst()).append(" against, ").append(abstained).append(" abstained, ")
                .append(didNotVote).append(" did not vote, ").append(v.resultAbsent()).append(" absent.</p></section>");
        b.append("<section lang=\"ru\"><p>Поимённое голосование в Рийгикогу")
                .append(when != null ? " " + when : "").append(": за ").append(v.resultInFavor()).append(", против ")
                .append(v.resultAgainst()).append(", воздержались ").append(abstained).append(", не голосовали ")
                .append(didNotVote).append(", отсутствовали ").append(v.resultAbsent()).append(".</p></section>");
        String desc = title + (when != null ? " (" + when + ")" : "") + ": poolt " + v.resultInFavor()
                + ", vastu " + v.resultAgainst() + ", erapooletuid " + abstained + ", ei hääletanud " + didNotVote + ".";
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
