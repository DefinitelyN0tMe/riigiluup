package com.riigiluup.admin;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Fail-fast guard so a deploy can't silently come up with an internet-reachable admin
 * panel behind the repo-default HTTP-Basic password. Active in every profile except
 * {@code local} (docker-compose dev default) and {@code test} (integration tests):
 * refuse to start unless admin is protected by Google OIDC OR a non-default password
 * is set. The {@code prod} profile is stricter still — Basic-only is not allowed there,
 * Google OIDC must be configured.
 */
@Component
public class AdminSecurityGuard {

    private final String adminPassword;
    private final String googleClientId;
    private final Environment env;

    public AdminSecurityGuard(@Value("${riigiluup.admin.password:}") String adminPassword,
                              @Value("${GOOGLE_OAUTH_CLIENT_ID:}") String googleClientId,
                              Environment env) {
        this.adminPassword = adminPassword;
        this.googleClientId = googleClientId;
        this.env = env;
    }

    @PostConstruct
    void verify() {
        List<String> profiles = Arrays.asList(env.getActiveProfiles());
        if (profiles.contains("local") || profiles.contains("test")) return;

        boolean oidc = googleClientId != null && !googleClientId.isBlank();
        boolean weakPassword = adminPassword == null || adminPassword.isBlank()
                || adminPassword.toLowerCase(Locale.ROOT).startsWith("change-me");

        if (profiles.contains("prod") && !oidc) {
            throw new IllegalStateException(
                    "Refusing to start in the 'prod' profile with a Basic-only admin panel: "
                    + "Google OIDC (GOOGLE_OAUTH_CLIENT_ID / GOOGLE_OAUTH_CLIENT_SECRET) must be "
                    + "configured for prod deploys.");
        }
        if (!oidc && weakPassword) {
            throw new IllegalStateException(
                    "Refusing to start with an unprotected admin panel: neither Google OIDC "
                    + "(GOOGLE_OAUTH_CLIENT_ID) nor a non-default RIIGILUUP_ADMIN_PASSWORD is "
                    + "configured. Set one, or run with the 'local' profile for development.");
        }
    }
}
