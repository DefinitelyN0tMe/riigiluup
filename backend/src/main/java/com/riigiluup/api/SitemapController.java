package com.riigiluup.api;

import com.riigiluup.group.GroupRepository;
import com.riigiluup.group.GroupType;
import com.riigiluup.initiative.InitiativeRepository;
import com.riigiluup.legislation.LegislativeItemRepository;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.vote.VoteEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dynamic {@code /sitemap.xml} (served unauthenticated via nginx, like {@code /healthz}) so the whole
 * data site — every MP, vote, bill, initiative, committee and group page — is discoverable and always
 * current, without a build-time generation step. Emits only public URLs, no data. Well under the
 * 50k-URL / 50MB single-sitemap limit at current volumes.
 */
@RestController
@RequiredArgsConstructor
public class SitemapController {

    private static final String BASE = "https://riigiluup.ee";
    private static final List<String> TOP_LEVEL = List.of(
            "/", "/politicians", "/votes", "/legislation", "/initiatives", "/committees", "/groups",
            "/speeches", "/analytics", "/compare", "/data-status", "/methodology",
            "/methodology/participation", "/methodology/alignment", "/methodology/agreement",
            "/methodology/analytics", "/sources", "/about", "/corrections", "/privacy", "/terms");
    private static final Set<GroupType> COMMITTEE_TYPES =
            Set.of(GroupType.STANDING_COMMITTEE, GroupType.SPECIAL_COMMITTEE);
    private static final Set<GroupType> GROUP_TYPES =
            Set.of(GroupType.DELEGATION, GroupType.ASSOCIATION, GroupType.BILATERAL_GROUP);

    private final PlenaryMemberRepository memberRepo;
    private final VoteEventRepository voteRepo;
    private final LegislativeItemRepository itemRepo;
    private final InitiativeRepository initiativeRepo;
    private final GroupRepository groupRepo;

    /** Built sitemap reused for an hour: it is ~1 MB / 12k URLs and was rebuilt on every request. */
    private static final long CACHE_MS = 3_600_000;
    private volatile String cached;
    private volatile long cachedAt;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public org.springframework.http.ResponseEntity<String> sitemap() {
        long now = System.currentTimeMillis();
        String body = cached;
        if (body == null || now - cachedAt > CACHE_MS) {
            body = build();
            cached = body;
            cachedAt = now;
        }
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePublic())
                .body(body);
    }

    private String build() {
        StringBuilder sb = new StringBuilder(1 << 16);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        TOP_LEVEL.forEach(p -> loc(sb, p));
        // <lastmod> tells crawlers which of the ~12k pages actually changed, so new and updated
        // votes, bills and profiles get re-crawled first instead of waiting their turn.
        for (Object[] r : memberRepo.findSlugsWithLastVoteForSitemap()) loc(sb, "/politicians/" + r[0], r[1]);
        for (Object[] r : voteRepo.findIdsWithDateForSitemap()) loc(sb, "/votes/" + r[0], r[1]);
        for (Object[] r : itemRepo.findIdsWithUpdatedForSitemap()) loc(sb, "/legislation/" + r[0], r[1]);
        for (Object[] r : initiativeRepo.findIdsWithLastEventForSitemap()) loc(sb, "/initiatives/" + r[0], r[1]);

        Set<GroupType> types = new HashSet<>();
        types.addAll(COMMITTEE_TYPES);
        types.addAll(GROUP_TYPES);
        for (Object[] row : groupRepo.findExternalIdAndTypeForSitemap(types)) {
            String ext = (String) row[0];
            GroupType t = (GroupType) row[1];
            loc(sb, (COMMITTEE_TYPES.contains(t) ? "/committees/" : "/groups/") + ext);
        }

        sb.append("</urlset>\n");
        return sb.toString();
    }

    private static void loc(StringBuilder sb, String path) {
        sb.append("  <url><loc>").append(BASE).append(xmlEscape(path)).append("</loc></url>\n");
    }

    /** A URL with its last-modified date (W3C date, Tallinn day); no lastmod when unknown. */
    static void loc(StringBuilder sb, String path, Object when) {
        java.time.Instant t = toInstant(when);
        sb.append("  <url><loc>").append(BASE).append(xmlEscape(path)).append("</loc>");
        if (t != null) {
            sb.append("<lastmod>").append(t.atZone(java.time.ZoneId.of("Europe/Tallinn")).toLocalDate()).append("</lastmod>");
        }
        sb.append("</url>\n");
    }

    private static java.time.Instant toInstant(Object o) {
        if (o instanceof java.time.Instant i) return i;
        if (o instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        if (o instanceof java.sql.Timestamp ts) return ts.toInstant();
        if (o instanceof java.time.LocalDateTime ldt) return ldt.atZone(java.time.ZoneOffset.UTC).toInstant();
        return null;
    }

    /** Slugs and UUIDs are URL-safe; still XML-escape defensively so no value can break the doc. */
    private static String xmlEscape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
