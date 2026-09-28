package com.riigiluup.seo;

import com.riigiluup.legislation.LegislativeItemRepository;
import com.riigiluup.vote.VoteEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tells IndexNow search engines (Bing, Yandex, Seznam, Naver...; Bing also feeds ChatGPT search)
 * about new and changed public pages right after an import, instead of waiting for the next crawl.
 * Sends only public page URLs of this site, no data. The key is public by design: it is served as
 * {@code https://riigiluup.ee/<key>.txt} (frontend/public) so the engines can check the site owns it.
 * Reads INDEXNOW_KEY from the environment; blank/unset = disabled (dev and CI never submit).
 * {@link #submitChangedSince} NEVER throws: a failed ping must not break the import that called it.
 */
@Slf4j
@Service
public class IndexNowService {

    private static final String HOST = "riigiluup.ee";
    private static final String BASE = "https://" + HOST;
    private static final String ENDPOINT = "https://api.indexnow.org/indexnow";
    /** Protocol limit per request. */
    private static final int MAX_URLS = 10_000;

    private final String key;
    private final VoteEventRepository voteRepo;
    private final LegislativeItemRepository itemRepo;
    private final RestClient rest = RestClient.builder().requestFactory(timeoutFactory()).build();

    public IndexNowService(@Value("${INDEXNOW_KEY:}") String key,
                           VoteEventRepository voteRepo,
                           LegislativeItemRepository itemRepo) {
        this.key = key == null ? "" : key.trim();
        this.voteRepo = voteRepo;
        this.itemRepo = itemRepo;
        if (this.key.isBlank()) {
            log.info("IndexNow disabled (INDEXNOW_KEY not set)");
        }
    }

    /**
     * Submit votes first imported since {@code since}, the bills those votes belong to, and bills
     * inserted or changed since then, plus the list pages that now show them. Nothing new = no request.
     */
    public void submitChangedSince(Instant since) {
        if (key.isBlank()) return;
        try {
            Set<String> urls = new LinkedHashSet<>();
            for (Object[] row : voteRepo.findIdsAndBillIdsImportedSince(since)) {
                urls.add(BASE + "/votes/" + row[0]);
                if (row[1] != null) urls.add(BASE + "/legislation/" + row[1]);
            }
            for (UUID id : itemRepo.findIdsUpdatedSince(since)) {
                urls.add(BASE + "/legislation/" + id);
            }
            if (urls.isEmpty()) return;
            urls.add(BASE + "/");
            urls.add(BASE + "/votes");
            urls.add(BASE + "/legislation");
            submit(new ArrayList<>(urls));
        } catch (Exception e) {
            log.warn("IndexNow submission failed: {}", e.toString());
        }
    }

    private void submit(List<String> urls) {
        for (int i = 0; i < urls.size(); i += MAX_URLS) {
            List<String> chunk = urls.subList(i, Math.min(urls.size(), i + MAX_URLS));
            var resp = rest.post()
                    .uri(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "host", HOST,
                            "key", key,
                            "keyLocation", BASE + "/" + key + ".txt",
                            "urlList", chunk))
                    .retrieve()
                    .toBodilessEntity();
            log.info("IndexNow: submitted {} url(s), HTTP {}", chunk.size(), resp.getStatusCode().value());
        }
    }

    private static SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5_000);
        f.setReadTimeout(20_000);
        return f;
    }
}
