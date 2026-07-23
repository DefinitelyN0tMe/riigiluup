package com.riigiluup.analytics.umami;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the admin "Analytics" panel needs, assembled from a handful of Umami calls.
 *
 * <p>{@code configured=false} → analytics is not wired up (blank creds); the panel shows a hint.
 * A non-null {@code error} → Umami was reachable but a call failed; the panel shows the message.
 */
public record AdminAnalyticsDto(
        boolean configured,
        String error,
        String range,
        Metric pageviews,
        Metric visitors,
        Metric visits,
        Double bounceRate,        // 0..1
        Double avgVisitSeconds,
        int activeVisitors,
        List<Point> series,
        List<Item> topPages,
        List<Item> topReferrers
) {
    public record Metric(long value, long prev) {}
    public record Point(String t, long pageviews, long sessions) {}
    public record Item(String label, long count) {}

    public static AdminAnalyticsDto notConfigured() {
        return new AdminAnalyticsDto(false, null, null, null, null, null, null, null, 0,
                List.of(), List.of(), List.of());
    }

    public static AdminAnalyticsDto error(String message) {
        return new AdminAnalyticsDto(true, message, null, null, null, null, null, null, 0,
                List.of(), List.of(), List.of());
    }

    public static AdminAnalyticsDto from(String range, JsonNode stats, JsonNode pv,
                                         JsonNode urls, JsonNode referrers, int active) {
        Metric pageviews = metric(stats, "pageviews");
        Metric visitors = metric(stats, "visitors");
        Metric visits = metric(stats, "visits");
        long bounces = value(stats, "bounces");
        long totaltime = value(stats, "totaltime");

        Double bounceRate = visits.value() > 0 ? (double) bounces / visits.value() : null;
        // Umami totaltime is seconds spent across non-bounced visits.
        long engaged = Math.max(0, visits.value() - bounces);
        Double avgVisit = engaged > 0 ? (double) totaltime / engaged : null;

        return new AdminAnalyticsDto(
                true, null, range,
                pageviews, visitors, visits,
                bounceRate, avgVisit, active,
                series(pv), items(urls), items(referrers));
    }

    /** A stats field is either {"value":n,"prev":m} (Umami v2) or a bare number. */
    private static Metric metric(JsonNode stats, String field) {
        if (stats == null) return new Metric(0, 0);
        JsonNode n = stats.get(field);
        if (n == null) return new Metric(0, 0);
        if (n.isObject()) return new Metric(n.path("value").asLong(0), n.path("prev").asLong(0));
        return new Metric(n.asLong(0), 0);
    }

    private static long value(JsonNode stats, String field) {
        return metric(stats, field).value();
    }

    private static List<Point> series(JsonNode pv) {
        List<Point> out = new ArrayList<>();
        if (pv == null) return out;
        JsonNode pageviews = pv.path("pageviews");
        JsonNode sessions = pv.path("sessions");
        int n = pageviews.isArray() ? pageviews.size() : 0;
        for (int i = 0; i < n; i++) {
            JsonNode p = pageviews.get(i);
            String t = p.path("x").asText("");
            long s = (sessions.isArray() && i < sessions.size()) ? sessions.get(i).path("y").asLong(0) : 0;
            out.add(new Point(t, p.path("y").asLong(0), s));
        }
        return out;
    }

    private static List<Item> items(JsonNode arr) {
        List<Item> out = new ArrayList<>();
        if (arr == null || !arr.isArray()) return out;
        for (JsonNode it : arr) {
            String label = it.path("x").asText("");
            out.add(new Item(label.isBlank() ? "(direct)" : label, it.path("y").asLong(0)));
        }
        return out;
    }
}
