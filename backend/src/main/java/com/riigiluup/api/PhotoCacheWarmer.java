package com.riigiluup.api;

import com.riigiluup.common.PhotoUrlRewriter;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Warms the photo proxy cache shortly after startup so the first visitor to the MP grid doesn't
 * trigger a cold, throttled fetch of every portrait. Runs on a background daemon thread. On a
 * populated disk cache each {@code load} is a fast disk hit; only a genuinely empty cache does
 * throttled upstream fetches (one-time). Failures are ignored — the on-demand path handles them.
 */
@Slf4j
@Component
class PhotoCacheWarmer {

    private final FileProxyController.Loader loader;
    private final PlenaryMemberRepository memberRepo;
    private final PhotoUrlRewriter photoUrls;
    private final boolean enabled;

    PhotoCacheWarmer(FileProxyController.Loader loader,
                     PlenaryMemberRepository memberRepo,
                     PhotoUrlRewriter photoUrls,
                     @Value("${riigiluup.files.warm-on-startup:true}") boolean enabled) {
        this.loader = loader;
        this.memberRepo = memberRepo;
        this.photoUrls = photoUrls;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!enabled) return;
        Thread t = new Thread(this::run, "photo-cache-warmer");
        t.setDaemon(true);
        t.start();
    }

    private void run() {
        try {
            List<PlenaryMember> members = memberRepo.findByActiveTrueOrderByLastNameAscFirstNameAsc();
            int ok = 0, skipped = 0;
            for (PlenaryMember m : members) {
                String uuid = photoUrls.fileUuid(m.getPhotoUrl());
                if (uuid == null) { skipped++; continue; }
                try {
                    loader.load(uuid);   // through the @Cacheable proxy → warms memory + disk
                    ok++;
                } catch (Exception e) {
                    skipped++;           // missing/failed photo — on-demand path will retry/404 later
                }
            }
            log.info("photo cache warm complete: {} warmed, {} skipped", ok, skipped);
        } catch (Exception e) {
            log.warn("photo cache warm aborted: {}", e.toString());
        }
    }
}
