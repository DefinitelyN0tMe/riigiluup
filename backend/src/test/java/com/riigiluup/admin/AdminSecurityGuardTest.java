package com.riigiluup.admin;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The startup guard must be active in every profile except {@code local} and {@code test},
 * and the {@code prod} profile must additionally refuse to boot Basic-only (OIDC required).
 */
class AdminSecurityGuardTest {

    private static AdminSecurityGuard guard(String password, String googleClientId, String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return new AdminSecurityGuard(password, googleClientId, env);
    }

    @Test
    void local_and_test_profiles_keep_the_convenient_defaults() {
        assertThatCode(() -> guard("change-me", "", "local").verify()).doesNotThrowAnyException();
        assertThatCode(() -> guard("change-me", "", "test", "local").verify()).doesNotThrowAnyException();
    }

    @Test
    void default_boot_with_the_repo_default_password_refuses_to_start() {
        assertThatThrownBy(() -> guard("change-me", "").verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unprotected admin panel");
    }

    @Test
    void default_boot_with_a_blank_password_refuses_to_start() {
        assertThatThrownBy(() -> guard("", "").verify())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void non_prod_boot_with_a_real_password_is_allowed() {
        assertThatCode(() -> guard("s3cret-enough", "").verify()).doesNotThrowAnyException();
    }

    @Test
    void prod_without_oidc_refuses_to_start_even_with_a_strong_password() {
        assertThatThrownBy(() -> guard("s3cret-enough", "", "prod").verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google OIDC");
    }

    @Test
    void prod_with_oidc_configured_starts() {
        assertThatCode(() -> guard("s3cret-enough", "client-id.apps.googleusercontent.com", "prod").verify())
                .doesNotThrowAnyException();
    }
}
