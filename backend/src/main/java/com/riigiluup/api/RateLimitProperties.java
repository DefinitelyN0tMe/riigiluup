package com.riigiluup.api;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Rate-limit configuration. Supports several (pattern, per-minute) rules with separate
 * buckets; the first rule whose pattern matches the request path wins. When no explicit
 * rules are configured the legacy single-rule behaviour ({@code pathRegex} + {@code perMinute})
 * is used as a fallback.
 */
@Component
@ConfigurationProperties(prefix = "riigiluup.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;
    /** Legacy fallback limit, used when no {@code rules} are configured. */
    private int perMinute = 60;
    /** Legacy fallback path pattern, used when no {@code rules} are configured. */
    private String pathRegex = "/api/v1/(?!admin/).*";
    private List<Rule> rules = new ArrayList<>();

    @Getter
    @Setter
    public static class Rule {
        /** Bucket name — part of the counter key so rules never share a bucket. */
        private String name;
        private String pattern;
        private int perMinute;
    }
}
