package com.riigiluup.admin;

import com.riigiluup.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end auth check against the real Spring Security filter chain.
 * Uses TestRestTemplate on the RANDOM_PORT server started by {@link AbstractIntegrationTest}.
 *
 * Note: OAuth2 autoconfig is excluded via application-test.yml, and the
 * "local" profile is active, so AdminSecurityConfig falls back to HTTP Basic.
 */
class AdminSecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_STATUS = "/api/v1/admin/status";
    private static final String PUBLIC_POLITICIANS = "/api/v1/politicians";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void admin_endpoint_without_auth_returns_401() {
        ResponseEntity<String> response = restTemplate.getForEntity(ADMIN_STATUS, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void admin_endpoint_with_wrong_credentials_returns_401() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("wrong", "credentials")
                .getForEntity(ADMIN_STATUS, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void admin_endpoint_with_correct_credentials_returns_200_and_status_json() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("testadmin", "testpass")
                .getForEntity(ADMIN_STATUS, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("status response should carry the expected JSON envelope")
                .contains("jobs")
                .contains("snapshotsByEntity")
                .contains("counts");
    }

    @Test
    void public_endpoint_reachable_without_auth() {
        ResponseEntity<String> response = restTemplate
                .getForEntity(PUBLIC_POLITICIANS, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
