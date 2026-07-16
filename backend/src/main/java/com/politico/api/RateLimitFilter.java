package com.politico.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Fixed-window per-IP rate limit for public API (60/min by default).
 * Caffeine-backed, evicts entries after the window closes. Admin endpoints excluded via path-regex.
 */
@Component
@Order(Integer.MIN_VALUE + 10)
@ConditionalOnProperty(value = "politico.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, AtomicInteger> hits;
    private final int perMinute;
    private final Pattern pathRegex;

    public RateLimitFilter(
            @Value("${politico.rate-limit.per-minute:60}") int perMinute,
            @Value("${politico.rate-limit.path-regex:/api/v1/(?!admin/).*}") String pathRegex
    ) {
        this.perMinute = perMinute;
        this.pathRegex = Pattern.compile(pathRegex);
        this.hits = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(50_000)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        if (!pathRegex.matcher(path).matches()) {
            chain.doFilter(req, res);
            return;
        }
        String ip = clientIp(req);
        AtomicInteger counter = hits.get(ip, k -> new AtomicInteger());
        int current = counter.incrementAndGet();
        if (current > perMinute) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.setHeader("Retry-After", "60");
            res.getWriter().write("{\"error\":\"rate_limit_exceeded\",\"perMinute\":" + perMinute + "}");
            return;
        }
        res.setHeader("X-RateLimit-Limit", String.valueOf(perMinute));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, perMinute - current)));
        chain.doFilter(req, res);
    }

    private static String clientIp(HttpServletRequest req) {
        // Use X-Real-IP, which the single trusted edge nginx sets to the real client address and
        // OVERWRITES on every request (deploy/nginx/politico.conf proxies /api/ straight to the api
        // in one hop). The leftmost X-Forwarded-For entry must NOT be used: nginx appends to it, so
        // its head is whatever the client sent — an attacker could rotate it to dodge the limit.
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return req.getRemoteAddr();
    }
}
