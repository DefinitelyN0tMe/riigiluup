package com.riigiluup.api;

import com.riigiluup.initiative.InitiativeRepository;
import com.riigiluup.legislation.LegislativeItemRepository;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.vote.VoteEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * Per-entity social-preview shells. nginx routes ONLY social/search crawler user-agents on entity
 * paths here (humans get the normal SPA from the web container); the response is the real built
 * index.html with its og:title / og:description / og:url / &lt;title&gt; / description rewritten for
 * the specific MP / vote / bill / initiative, so a shared deep link renders a distinct card instead
 * of the generic homepage one. It stays a full working SPA shell (real hashed asset tags), so even a
 * mis-routed human still gets a working page. All injected values are HTML-escaped.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OgShellController {

    private static final String BASE = "https://riigiluup.ee";
    // Exact strings from index.html (shared by <title>/og:title/twitter:title and the three
    // description metas), so one replace() updates all consistent copies.
    private static final String BASE_TITLE = "Riigiluup — Riigikogu läbipaistvus";
    private static final String BASE_DESC =
            "Riigikogu avaandmed loetavaks: kuidas iga saadik hääletab, mida ta algatab ja kus on kandideerinud. Faktid, mitte hinnangud.";
    private static final String OG_URL_TAG = "<meta property=\"og:url\" content=\"https://riigiluup.ee\" />";

    private final PlenaryMemberRepository memberRepo;
    private final VoteEventRepository voteRepo;
    private final LegislativeItemRepository itemRepo;
    private final InitiativeRepository initiativeRepo;
    private final PoliticianProfileController profileApi;
    private final VoteController voteApi;
    private final LegislationController legislationApi;
    private final PoliticianController politicianApi;
    private final com.riigiluup.committee.CommitteeService committeeService;
    private final com.riigiluup.group.GroupDirectoryService groupService;

    /**
     * Rendered crawler pages, 10 min. Crawlers are the only callers; a profile page costs ~1-1.5 s of
     * DB work, so a crawl of the ~12k sitemap URLs would otherwise load the database needlessly. The
     * data changes at most every 6 h, so 10 min of staleness is invisible.
     */
    private final com.github.benmanes.caffeine.cache.Cache<String, String> pageCache =
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                    .maximumSize(3_000)
                    .expireAfterWrite(java.time.Duration.ofMinutes(10))
                    .build();

    private final RestClient web = RestClient.builder()
            .requestFactory(timeoutFactory())
            .build();
    private volatile String cachedShell;
    private volatile long cachedAt;

    @GetMapping(value = "/politicians/{slug}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String politician(@PathVariable String slug) {
        String path = "/politicians/" + slug;
        return rendered(path, () -> {
            var dto = profileApi.get(slug).getBody();
            return dto == null ? null : CrawlerContent.politician(dto);
        }, () -> memberRepo.findBySlug(slug)
                .map(m -> shell(m.getFullName(),
                        m.getFactionName() != null ? m.getFactionName() : "Riigikogu liige", path))
                .orElseGet(() -> shell(null, null, path)));
    }

    @GetMapping(value = "/votes/{id}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String vote(@PathVariable UUID id) {
        String path = "/votes/" + id;
        return rendered(path, () -> {
            var dto = voteApi.detail(id).getBody();
            return dto == null ? null : CrawlerContent.vote(dto);
        }, () -> voteRepo.findById(id)
                .map(v -> shell(v.getDescription(), "Nimeline hääletus Riigikogus", path))
                .orElseGet(() -> shell(null, null, path)));
    }

    @GetMapping(value = "/legislation/{id}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String legislation(@PathVariable UUID id) {
        String path = "/legislation/" + id;
        return rendered(path, () -> {
            var dto = legislationApi.detail(id).getBody();
            return dto == null ? null : CrawlerContent.legislation(dto);
        }, () -> itemRepo.findById(id)
                .map(i -> shell(i.getTitle(), "Eelnõu menetlus Riigikogus", path))
                .orElseGet(() -> shell(null, null, path)));
    }

    @GetMapping(value = "/initiatives/{id}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String initiative(@PathVariable Long id) {
        String path = "/initiatives/" + id;
        return rendered(path, () -> initiativeRepo.findById(id).map(CrawlerContent::initiative).orElse(null),
                () -> shell(null, null, path));
    }


    // ---------------------------------------------------------------- hub pages (crawlers only, via nginx)

    @GetMapping(value = "/", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String home() {
        return rendered("/", () -> {
            var factions = politicianApi.factions().stream()
                    .map(f -> new CrawlerContent.NamedCount(f.name(), f.memberCount())).toList();
            long active = memberRepo.countByActiveTrue();
            return CrawlerContent.home(factions, active, recentBills(), recentVotes());
        }, () -> baseShell());
    }

    @GetMapping(value = "/politicians", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String politicians() {
        return rendered("/politicians", () -> CrawlerContent.politiciansList(
                memberRepo.findByActiveTrueOrderByLastNameAscFirstNameAsc().stream()
                        .map(m -> new CrawlerContent.MpLink(m.getSlug(), m.getFullName(), m.getFactionName()))
                        .toList()), () -> shell(null, null, "/politicians"));
    }

    @GetMapping(value = "/legislation", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String legislationList() {
        return rendered("/legislation", () -> CrawlerContent.billsList(recentBills()),
                () -> shell(null, null, "/legislation"));
    }

    @GetMapping(value = "/votes", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String votesList() {
        return rendered("/votes", () -> CrawlerContent.votesList(recentVotes()), () -> shell(null, null, "/votes"));
    }

    @GetMapping(value = "/committees/{externalId}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String committee(@PathVariable String externalId) {
        String path = "/committees/" + externalId;
        return rendered(path, () -> committeeService.detail(externalId).map(CrawlerContent::committee).orElse(null),
                () -> shell(null, null, path));
    }

    @GetMapping(value = "/groups/{externalId}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String group(@PathVariable String externalId) {
        String path = "/groups/" + externalId;
        return rendered(path, () -> groupService.detail(externalId).map(CrawlerContent::group).orElse(null),
                () -> shell(null, null, path));
    }

    private List<CrawlerContent.BillLink> recentBills() {
        return itemRepo.findTop50ByInitiatedDateIsNotNullOrderByInitiatedDateDesc().stream()
                .map(i -> new CrawlerContent.BillLink(i.getId(), i.getMark(), i.getDraftTypeCode(), i.getTitle(), i.getInitiatedDate()))
                .toList();
    }

    private List<CrawlerContent.VoteLink> recentVotes() {
        return voteRepo.findTop50ByStartedAtIsNotNullOrderByStartedAtDesc().stream()
                .map(v -> new CrawlerContent.VoteLink(v.getId(), v.getDescription(), v.getStartedAt()))
                .toList();
    }

    /**
     * Full crawler page: the entity's meta tags plus a static fact block in #root and JSON-LD.
     * Anything unexpected (entity missing, a DTO throwing) falls back to the previous meta-only
     * shell, so a rendering bug can never turn a crawler visit into an error page.
     */
    private String rendered(String path, java.util.function.Supplier<CrawlerContent.Page> page,
                            java.util.function.Supplier<String> fallback) {
        String hit = pageCache.getIfPresent(path);
        if (hit != null) return hit;
        try {
            CrawlerContent.Page p = page.get();
            if (p == null) return fallback.get();
            // The home page keeps the base title as is; entity/hub titles get the " — Riigiluup" suffix once.
            String title = BASE_TITLE.equals(p.title()) ? null : p.title().replace(" — Riigiluup", "");
            String html = shell(title, p.description(), path);
            if (html.contains(ROOT_DIV)) {
                html = html.replace(ROOT_DIV, "<div id=\"root\">" + p.bodyHtml() + "</div>");
            } else {
                log.warn("og-shell: base index.html has no empty #root div, fact block not injected");
            }
            if (p.jsonLd() != null && html.contains("</head>")) {
                html = html.replace("</head>",
                        "  <script type=\"application/ld+json\">" + p.jsonLd() + "</script>\n  </head>");
            }
            pageCache.put(path, html);
            return html;
        } catch (Exception e) {
            log.warn("og-shell: crawler render failed for {}: {}", path, e.toString());
            return fallback.get();
        }
    }

    private static final String ROOT_DIV = "<div id=\"root\"></div>";

    /** Rewrite the base shell's meta tags for one entity. Null title/desc -> generic (homepage) card. */
    private String shell(String title, String desc, String path) {
        String html = baseShell();
        if (title != null && !title.isBlank()) {
            html = html.replace(BASE_TITLE, esc(title.trim() + " — Riigiluup"));
        }
        if (desc != null && !desc.isBlank()) {
            html = html.replace(BASE_DESC, esc(desc.trim()));
        }
        String url = esc(BASE + path);
        html = html.replace(OG_URL_TAG,
                "<meta property=\"og:url\" content=\"" + url + "\" />\n"
                + "    <link rel=\"canonical\" href=\"" + url + "\" />");
        return html;
    }

    /** The real built index.html from the web container, cached ~5 min (only crawlers hit this). */
    private String baseShell() {
        long now = System.currentTimeMillis();
        String c = cachedShell;
        if (c != null && now - cachedAt < 300_000L) return c;
        try {
            // Fetch bytes and decode UTF-8 explicitly: the web response has no charset in its
            // Content-Type, so RestClient would default to ISO-8859-1 and mangle the em-dash / ä in
            // the tags we match on, so the replace() would silently no-op.
            byte[] bytes = web.get().uri("http://web:80/index.html").retrieve().body(byte[].class);
            if (bytes != null) {
                String fetched = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                if (fetched.contains("<html")) {
                    // Drift guard: if index.html's title/description/og:url literals ever change
                    // (e.g. a brand-casing edit), the replace()s below would silently no-op and every
                    // shared card would go generic with no error. Make that loud instead.
                    if (!fetched.contains(BASE_TITLE) || !fetched.contains(BASE_DESC)
                            || !fetched.contains(OG_URL_TAG)) {
                        log.warn("og-shell: base index.html no longer contains an expected literal "
                                + "(title/desc/og:url) — per-entity rewrite will no-op; update the "
                                + "BASE_TITLE / BASE_DESC / OG_URL_TAG constants to match index.html");
                    }
                    cachedShell = fetched;
                    cachedAt = now;
                    return fetched;
                }
            }
        } catch (Exception e) {
            log.warn("og-shell: base index.html fetch failed: {}", e.getMessage());
        }
        return c != null ? c : "<!doctype html><html lang=\"et\"><head><title>Riigiluup</title></head><body></body></html>";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2_000);
        f.setReadTimeout(3_000);
        return f;
    }
}
