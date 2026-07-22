package com.riigiluup.admin;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
    private final String dbPassword;
    private final Environment env;

    public AdminSecurityGuard(@Value("${riigiluup.admin.password:}") String adminPassword,
                              @Value("${GOOGLE_OAUTH_CLIENT_ID:}") String googleClientId,
                              @Value("${spring.datasource.password:}") String dbPassword,
                              Environment env) {
        this.adminPassword = adminPassword;
        this.googleClientId = googleClientId;
        this.dbPassword = dbPassword;
        this.env = env;
    }

    @PostConstruct
    void verify() {
        // acceptsProfiles (not getActiveProfiles) so the spring.profiles.default=local
        // fallback for bare bootRun is honoured too.
        if (env.acceptsProfiles(Profiles.of("local", "test"))) return;
        List<String> profiles = Arrays.asList(env.getActiveProfiles());

        boolean oidc = googleClientId != null && !googleClientId.isBlank();
        boolean weakPassword = adminPassword == null || adminPassword.isBlank()
                || adminPassword.toLowerCase(Locale.ROOT).startsWith("change-me");

        if (profiles.contains("prod") && !oidc) {
            throw new IllegalStateException(
                    "Refusing to start in the 'prod' profile with a Basic-only admin panel: "
                    + "Google OIDC (GOOGLE_OAUTH_CLIENT_ID / GOOGLE_OAUTH_CLIENT_SECRET) must be "
                    + "configured for prod deploys.");
        }
        if (profiles.contains("prod")) {
            String db = dbPassword == null ? "" : dbPassword.toLowerCase(Locale.ROOT);
            // "riigiluup" is the dev-compose default, "change-me*" the .env example value.
            if (db.isBlank() || db.equals("riigiluup") || db.startsWith("change-me")) {
                throw new IllegalStateException(
                        "Refusing to start in the 'prod' profile with a default/blank database "
                        + "password: set a generated POSTGRES_PASSWORD in the deploy .env.");
            }
        }
        if (!oidc && weakPassword) {
            throw new IllegalStateException(
                    "Refusing to start with an unprotected admin panel: neither Google OIDC "
                    + "(GOOGLE_OAUTH_CLIENT_ID) nor a non-default RIIGILUUP_ADMIN_PASSWORD is "
                    + "configured. Set one, or run with the 'local' profile for development.");
        }
    }
}
