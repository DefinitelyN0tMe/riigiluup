package com.politico.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Picks the Postgres backend for the integration test suite:
 *
 * <ol>
 *   <li>If {@code POLITICO_IT_JDBC_URL} is set → connect to that URL directly.
 *       Intended for Windows dev boxes where docker-java's TCP probe fails
 *       against Docker Desktop 4.73.</li>
 *   <li>Otherwise → boot a shared {@code postgres:16-alpine} Testcontainer.
 *       Used on Linux CI runners with a working Docker socket.</li>
 * </ol>
 *
 * The container/URL is registered as {@code spring.datasource.*} on a
 * property source that outranks {@code application-*.yml}.
 */
public class IntegrationDbSelector
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String EXTERNAL_JDBC_URL = System.getenv("POLITICO_IT_JDBC_URL");
    private static final String EXTERNAL_USER =
            System.getenv().getOrDefault("POLITICO_IT_JDBC_USER", "politico");
    private static final String EXTERNAL_PASSWORD =
            System.getenv().getOrDefault("POLITICO_IT_JDBC_PASSWORD", "politico");

    /** Reused across every context that runs during the JVM's lifetime. */
    private static volatile PostgreSQLContainer<?> sharedContainer;

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        Map<String, Object> props = new LinkedHashMap<>();
        if (EXTERNAL_JDBC_URL != null && !EXTERNAL_JDBC_URL.isBlank()) {
            props.put("spring.datasource.url", EXTERNAL_JDBC_URL);
            props.put("spring.datasource.username", EXTERNAL_USER);
            props.put("spring.datasource.password", EXTERNAL_PASSWORD);
            props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            props.put("politico.it.backend", "external");
        } else {
            PostgreSQLContainer<?> container = ensureContainer();
            props.put("spring.datasource.url", container.getJdbcUrl());
            props.put("spring.datasource.username", container.getUsername());
            props.put("spring.datasource.password", container.getPassword());
            props.put("spring.datasource.driver-class-name", container.getDriverClassName());
            props.put("politico.it.backend", "testcontainers");
        }
        ctx.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("integration-db", props));
    }

    private static PostgreSQLContainer<?> ensureContainer() {
        PostgreSQLContainer<?> local = sharedContainer;
        if (local == null) {
            synchronized (IntegrationDbSelector.class) {
                local = sharedContainer;
                if (local == null) {
                    local = new PostgreSQLContainer<>("postgres:16-alpine")
                            .withDatabaseName("politico_it")
                            .withUsername("politico")
                            .withPassword("politico")
                            .withReuse(true);
                    local.start();
                    // Wire a shutdown hook so the container stops when the
                    // JVM exits (Ryuk cleans up crashed test runs anyway).
                    Runtime.getRuntime().addShutdownHook(new Thread(local::stop));
                    sharedContainer = local;
                }
            }
        }
        return local;
    }
}
