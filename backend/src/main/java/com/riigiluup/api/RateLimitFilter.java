package com.riigiluup.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Fixed-window per-IP rate limit for the public API. Supports several (pattern, per-minute)
 * rules with separate buckets — e.g. default 60/min, files/* 300/min, analytics/* 20/min.
 * The first matching rule wins; a request matching no rule is not limited. Caffeine-backed,
 * entries evict after the window closes. Bucket key = IP + rule name so buckets never overlap.
 */
@Component
@Order(Integer.MIN_VALUE + 10)
@ConditionalOnProperty(value = "riigiluup.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, AtomicInteger> hits;
    private final List<CompiledRule> rules;

    private record CompiledRule(String name, Pattern pattern, int perMinute) {}

    public RateLimitFilter(RateLimitProperties props) {
        this.rules = compile(props);
        this.hits = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(50_000)
                .build();
    }

    private static List<CompiledRule> compile(RateLimitProperties props) {
        List<CompiledRule> out = new ArrayList<>();
        if (props.getRules() == null || props.getRules().isEmpty()) {
            // Fallback: preserve the legacy single-rule behaviour.
            out.add(new CompiledRule("default", Pattern.compile(props.getPathRegex()), props.getPerMinute()));
            return out;
        }
        java.util.Set<String> names = new java.util.HashSet<>();
        for (RateLimitProperties.Rule r : props.getRules()) {
            String name = (r.getName() == null || r.getName().isBlank()) ? r.getPattern() : r.getName();
            // Names are part of the bucket key — duplicates would silently share a counter.
            if (!names.add(name)) {
                throw new IllegalStateException("duplicate rate-limit rule name: " + name);
            }
            out.add(new CompiledRule(name, Pattern.compile(r.getPattern()), r.getPerMinute()));
        }
        return out;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        CompiledRule rule = null;
        for (CompiledRule r : rules) {
            if (r.pattern().matcher(path).matches()) { rule = r; break; }
        }
        if (rule == null) {
            chain.doFilter(req, res);
            return;
        }
        String ip = clientIp(req);
        // Bucket per (IP, rule): distinct rules never share a counter.
        AtomicInteger counter = hits.get(ip + "|" + rule.name(), k -> new AtomicInteger());
        int current = counter.incrementAndGet();
        if (current > rule.perMinute()) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.setHeader("Retry-After", "60");
            res.getWriter().write("{\"error\":\"rate_limit_exceeded\",\"perMinute\":" + rule.perMinute() + "}");
            return;
        }
        res.setHeader("X-RateLimit-Limit", String.valueOf(rule.perMinute()));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, rule.perMinute() - current)));
        chain.doFilter(req, res);
    }

    private static String clientIp(HttpServletRequest req) {
        // Use X-Real-IP, which the single trusted edge nginx sets to the real client address and
        // OVERWRITES on every request (deploy/nginx/riigiluup.conf proxies /api/ straight to the api
        // in one hop). The leftmost X-Forwarded-For entry must NOT be used: nginx appends to it, so
        // its head is whatever the client sent — an attacker could rotate it to dodge the limit.
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return req.getRemoteAddr();
    }
}
