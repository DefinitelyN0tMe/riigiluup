package com.riigiluup.admin;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Fail-fast guard so a production deploy can't silently come up with an internet-reachable admin
 * panel behind the repo-default HTTP-Basic password. When the {@code prod} profile is active,
 * refuse to start unless admin is protected by Google OIDC OR a non-default password is set.
 * Local/dev boots (no {@code prod} profile) keep the convenient defaults.
 */
@Component
@RequiredArgsConstructor
public class AdminSecurityGuard {

    @Value("${riigiluup.admin.password:}")
    private String adminPassword;

    @Value("${GOOGLE_OAUTH_CLIENT_ID:}")
    private String googleClientId;

    private final Environment env;

    @PostConstruct
    void verify() {
        boolean prod = Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (!prod) return;

        boolean oidc = googleClientId != null && !googleClientId.isBlank();
        boolean weakPassword = adminPassword == null || adminPassword.isBlank()
                || adminPassword.toLowerCase().startsWith("change-me");

        if (!oidc && weakPassword) {
            throw new IllegalStateException(
                    "Refusing to start in the 'prod' profile with an unprotected admin panel: "
                    + "neither Google OIDC (GOOGLE_OAUTH_CLIENT_ID) nor a non-default "
                    + "RIIGILUUP_ADMIN_PASSWORD is configured. Set one before deploying.");
        }
    }
}
