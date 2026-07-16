package com.riigiluup.common;

import org.springframework.stereotype.Component;

/**
 * Rewrites a Riigikogu file-download URL into our own proxy path so the SPA
 * loads MP photos through this backend (bypassing Riigikogu's per-IP limit).
 * Non-Riigikogu URLs are returned unchanged.
 */
@Component
public class PhotoUrlRewriter {

    private static final String FILES_SEGMENT = "/api/files/";

    public String toProxyPath(String storedUrl) {
        if (storedUrl == null || storedUrl.isBlank()) return null;
        int start = storedUrl.indexOf(FILES_SEGMENT);
        if (start < 0) return storedUrl;
        String tail = storedUrl.substring(start + FILES_SEGMENT.length());
        int slash = tail.indexOf('/');
        String uuid = slash < 0 ? tail : tail.substring(0, slash);
        if (uuid.isBlank()) return storedUrl;
        return "/api/v1/files/" + uuid;
    }
}
