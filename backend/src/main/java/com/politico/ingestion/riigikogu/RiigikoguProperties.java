package com.politico.ingestion.riigikogu;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "politico.riigikogu")
public record RiigikoguProperties(
        String baseUrl,
        String userAgent,
        int rateLimitPerSecond
) {}
