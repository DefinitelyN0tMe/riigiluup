package com.riigiluup.api;

import com.riigiluup.common.PhotoUrlRewriter;
import com.riigiluup.person.PlenaryMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * The Riigikogu file ids the site actually links to (MP portraits). The file proxy serves only
 * these, so a flood of random or harvested document ids never reaches the source's rate-limited
 * API or fills the disk cache. The set is small (about 130 ids) and reloaded at most once a minute
 * when an unknown id is asked for, so a newly imported MP's photo works within a minute.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class KnownPhotoIds {

    private static final long RELOAD_MIN_INTERVAL_MS = 60_000;

    private final PlenaryMemberRepository memberRepo;
    private final PhotoUrlRewriter photoUrls;

    private volatile Set<String> ids = Set.of();
    private volatile long loadedAt;

    boolean contains(String uuid) {
        String key = uuid.toLowerCase();
        if (ids.contains(key)) return true;
        long now = System.currentTimeMillis();
        if (now - loadedAt >= RELOAD_MIN_INTERVAL_MS) reload(now);
        return ids.contains(key);
    }

    private synchronized void reload(long now) {
        if (now - loadedAt < RELOAD_MIN_INTERVAL_MS) return;
        try {
            Set<String> fresh = new HashSet<>();
            for (String url : memberRepo.findAllPhotoUrls()) {
                String id = photoUrls.fileUuid(url);
                if (id != null) fresh.add(id.toLowerCase());
            }
            ids = Set.copyOf(fresh);
        } catch (Exception e) {
            log.warn("could not load known photo ids: {}", e.toString());
        } finally {
            loadedAt = now;
        }
    }
}
