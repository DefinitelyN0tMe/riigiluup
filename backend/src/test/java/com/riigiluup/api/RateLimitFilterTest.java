package com.riigiluup.api;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static RateLimitProperties props(RateLimitProperties.Rule... rules) {
        RateLimitProperties p = new RateLimitProperties();
        p.setRules(List.of(rules));
        return p;
    }

    private static RateLimitProperties.Rule rule(String name, String pattern, int perMinute) {
        RateLimitProperties.Rule r = new RateLimitProperties.Rule();
        r.setName(name);
        r.setPattern(pattern);
        r.setPerMinute(perMinute);
        return r;
    }

    /** Fire N requests from one IP at a path; return the count that were allowed (not 429). */
    private static int allowed(RateLimitFilter filter, String path, String ip, int n) throws Exception {
        AtomicInteger passed = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
            req.setRequestURI(path);
            req.addHeader("X-Real-IP", ip);
            MockHttpServletResponse res = new MockHttpServletResponse();
            FilterChain chain = (rq, rs) -> passed.incrementAndGet();
            filter.doFilter(req, res, chain);
        }
        return passed.get();
    }

    @Test
    void separate_rules_have_separate_limits() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(props(
                rule("files", "/api/v1/files/.*", 5),
                rule("analytics", "/api/v1/analytics/.*", 2),
                rule("default", "/api/v1/(?!admin/).*", 3)
        ));

        // files: 5 allowed, 6th blocked
        assertThat(allowed(filter, "/api/v1/files/abc", "1.1.1.1", 8)).isEqualTo(5);
        // analytics: stricter, only 2
        assertThat(allowed(filter, "/api/v1/analytics/night-votes", "1.1.1.1", 8)).isEqualTo(2);
        // default catch-all: 3
        assertThat(allowed(filter, "/api/v1/votes", "1.1.1.1", 8)).isEqualTo(3);
    }

    @Test
    void buckets_do_not_bleed_across_rules_for_same_ip() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(props(
                rule("files", "/api/v1/files/.*", 2),
                rule("default", "/api/v1/(?!admin/).*", 2)
        ));
        // Exhaust files bucket for this IP...
        assertThat(allowed(filter, "/api/v1/files/x", "9.9.9.9", 5)).isEqualTo(2);
        // ...default bucket for the same IP is untouched.
        assertThat(allowed(filter, "/api/v1/votes", "9.9.9.9", 5)).isEqualTo(2);
    }

    @Test
    void distinct_ips_get_independent_budgets() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(props(
                rule("default", "/api/v1/(?!admin/).*", 2)
        ));
        assertThat(allowed(filter, "/api/v1/votes", "1.1.1.1", 5)).isEqualTo(2);
        assertThat(allowed(filter, "/api/v1/votes", "2.2.2.2", 5)).isEqualTo(2);
    }

    @Test
    void first_matching_rule_wins() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(props(
                rule("files", "/api/v1/files/.*", 4),
                rule("default", "/api/v1/(?!admin/).*", 1)
        ));
        // files path matches the files rule (4), not the default (1), despite default also matching.
        assertThat(allowed(filter, "/api/v1/files/photo", "1.2.3.4", 6)).isEqualTo(4);
    }

    @Test
    void unmatched_paths_are_not_limited() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(props(
                rule("default", "/api/v1/(?!admin/).*", 1)
        ));
        // admin is excluded by the negative lookahead → never limited.
        assertThat(allowed(filter, "/api/v1/admin/imports", "1.1.1.1", 10)).isEqualTo(10);
    }

    @Test
    void empty_rules_fall_back_to_legacy_single_rule() throws Exception {
        RateLimitProperties p = new RateLimitProperties();
        p.setPerMinute(2);
        p.setPathRegex("/api/v1/(?!admin/).*");
        // no rules configured
        RateLimitFilter filter = new RateLimitFilter(p);
        assertThat(allowed(filter, "/api/v1/votes", "1.1.1.1", 5)).isEqualTo(2);
    }

    @Test
    void duplicate_rule_names_are_rejected_at_construction() {
        RateLimitProperties p = props(
                rule("dup", "/api/v1/a/.*", 10),
                rule("dup", "/api/v1/b/.*", 10));
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> new RateLimitFilter(p))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate");
    }
}
