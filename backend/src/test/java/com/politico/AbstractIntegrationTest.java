package com.politico;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests backed by a real PostgreSQL 16 instance.
 *
 * <p>Two subclasses provide the DB:
 * <ul>
 *   <li>{@link com.politico.support.TestcontainersDb} — Testcontainers path
 *       for Linux CI where the Docker daemon socket is reachable directly.</li>
 *   <li>{@link com.politico.support.ExternalDbConfig} — external DB via
 *       {@code POLITICO_IT_JDBC_URL} env var, an escape hatch for Windows
 *       dev boxes where Docker Desktop 4.73 / Engine 29.4 returns a stub
 *       HTTP 400 to non-CLI clients on TCP, breaking docker-java's probe.</li>
 * </ul>
 *
 * <p>The chosen backend is picked automatically by
 * {@link com.politico.support.IntegrationDbSelector} at context load time.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "local"})
@Tag("integration")
@org.springframework.test.context.ContextConfiguration(
        initializers = com.politico.support.IntegrationDbSelector.class)
public abstract class AbstractIntegrationTest {
}
