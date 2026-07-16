package com.riigiluup.ingestion.riigikogu;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "riigiluup.riigikogu")
public record RiigikoguProperties(
        String baseUrl,
        String userAgent,
        int rateLimitPerSecond
) {}
