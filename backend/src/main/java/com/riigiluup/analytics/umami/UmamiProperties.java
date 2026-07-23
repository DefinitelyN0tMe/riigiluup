package com.riigiluup.analytics.umami;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection to the self-hosted Umami instance (internal Docker network only — never
 * exposed publicly). The admin panel reads visitor stats through the backend so the
 * Umami dashboard itself needs no public origin and no second login: the existing
 * OIDC admin gate on {@code /api/v1/admin/**} is the only door.
 *
 * <p>Blank {@code password}/{@code websiteId} = analytics not configured; the endpoint
 * then returns {@code configured=false} instead of erroring, so a dev/local build works.
 */
@ConfigurationProperties(prefix = "riigiluup.umami")
public record UmamiProperties(
        String baseUrl,
        String username,
        String password,
        String websiteId
) {
}
