package com.riigiluup.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Boots a WireMock server on a random port and rewrites the Riigikogu base-url
 * property before Spring caches the {@code RiigikoguClient} bean.
 *
 * The trick: call {@link #start(String)} in a {@code @DynamicPropertySource}
 * method (as a lambda that captures the server), OR use the static helper for
 * boot-then-property patterns.
 */
public final class WireMockSupport {

    private WireMockSupport() {}

    public static WireMockServer newServer() {
        WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        return server;
    }

    /**
     * Load a fixture JSON string from {@code src/test/resources/fixtures/}
     * (classpath at test time).
     */
    public static String fixture(String path) {
        try {
            var url = WireMockSupport.class.getClassLoader().getResource(path);
            if (url == null) {
                throw new IllegalStateException("fixture not found on classpath: " + path);
            }
            Path p = Paths.get(url.toURI());
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("failed to read fixture " + path, e);
        }
    }
}
