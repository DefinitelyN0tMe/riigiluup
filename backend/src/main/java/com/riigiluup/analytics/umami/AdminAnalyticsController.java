package com.riigiluup.analytics.umami;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only proxy over Umami stats. Sits under {@code /api/v1/admin/**}, so it inherits the
 * OIDC + email-allowlist gate from {@link com.riigiluup.admin.AdminSecurityConfig} — no second
 * login, and Umami itself never has to be publicly reachable.
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AdminAnalyticsController.class);
    private static final long HOUR = 3_600_000L;
    private static final long DAY = 86_400_000L;

    private final UmamiClient umami;

    @GetMapping("/summary")
    public AdminAnalyticsDto summary(@RequestParam(defaultValue = "7d") String range) {
        if (!umami.configured()) return AdminAnalyticsDto.notConfigured();

        long now = System.currentTimeMillis();
        long start = switch (range) {
            case "24h" -> now - 24 * HOUR;
            case "30d" -> now - 30 * DAY;
            default -> now - 7 * DAY;   // "7d"
        };
        String unit = "24h".equals(range) ? "hour" : "day";

        try {
            return AdminAnalyticsDto.from(
                    range,
                    umami.stats(start, now),
                    umami.pageviews(start, now, unit, "Europe/Tallinn"),
                    umami.metrics(start, now, "path", 10),      // "path" (this Umami build); older builds used "url"
                    umami.metrics(start, now, "referrer", 10),
                    umami.activeVisitors());
        } catch (Exception e) {
            log.warn("Umami analytics fetch failed: {}", e.toString());
            return AdminAnalyticsDto.error(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }
}
