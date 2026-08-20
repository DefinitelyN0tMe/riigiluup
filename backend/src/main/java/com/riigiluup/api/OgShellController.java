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

    private final RestClient web = RestClient.builder()
            .requestFactory(timeoutFactory())
            .build();
    private volatile String cachedShell;
    private volatile long cachedAt;

    @GetMapping(value = "/politicians/{slug}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String politician(@PathVariable String slug) {
        return memberRepo.findBySlug(slug)
                .map(m -> shell(m.getFullName(),
                        m.getFactionName() != null ? m.getFactionName() : "Riigikogu liige",
                        "/politicians/" + slug))
                .orElseGet(() -> shell(null, null, "/politicians/" + slug));
    }

    @GetMapping(value = "/votes/{id}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String vote(@PathVariable UUID id) {
        return voteRepo.findById(id)
                .map(v -> shell(v.getDescription(), "Nimeline hääletus Riigikogus", "/votes/" + id))
                .orElseGet(() -> shell(null, null, "/votes/" + id));
    }

    @GetMapping(value = "/legislation/{id}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String legislation(@PathVariable UUID id) {
        return itemRepo.findById(id)
                .map(i -> shell(i.getTitle(), "Eelnõu menetlus Riigikogus", "/legislation/" + id))
                .orElseGet(() -> shell(null, null, "/legislation/" + id));
    }

    @GetMapping(value = "/initiatives/{id}", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String initiative(@PathVariable Long id) {
        return initiativeRepo.findById(id)
                .map(i -> shell(i.getTitle(), "Kollektiivne pöördumine Riigikogule", "/initiatives/" + id))
                .orElseGet(() -> shell(null, null, "/initiatives/" + id));
    }

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
