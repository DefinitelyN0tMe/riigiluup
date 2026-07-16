plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

jacoco { toolVersion = "0.8.12" }

group = "com.riigiluup"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories { mavenCentral() }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    // bucket4j starter removed 2026-07-13 — 0.12.x SPI required a specific cache-backend module
    // that never resolved cleanly with our Caffeine setup. Replaced by com.riigiluup.api.RateLimitFilter
    // (60 lines, Caffeine window, per-IP).
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.7")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.mapstruct:mapstruct:1.6.2")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.wiremock:wiremock-standalone:3.9.1")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform {
        // Fast task excludes legacy slow tests and the new "integration" tag.
        excludeTags("slow", "integration")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<Test>("slowTest") {
    useJUnitPlatform {
        includeTags("slow")
    }
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("integrationTest") {
    description = "Runs @Tag(\"integration\") tests against a real Postgres 16."
    group = "verification"
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter(tasks.test)
    // Keep a single fork so the shared Testcontainer/external DB is reused.
    maxParallelForks = 1
    // The DB backend is picked by com.riigiluup.support.IntegrationDbSelector:
    //   - Default: boots a shared postgres:16-alpine via Testcontainers
    //     (works on Linux CI / any host with a working Docker socket).
    //   - Windows dev escape hatch: set RIIGILUUP_IT_JDBC_URL to point at a
    //     pre-started Postgres. Needed on Docker Desktop 4.73 / Engine 29.4
    //     where docker-java's probe fails with HTTP 400 against the TCP
    //     proxy. Example:
    //       docker run -d --rm --name riigiluup-test-pg -p 25432:5432 \
    //         -e POSTGRES_DB=riigiluup_it -e POSTGRES_USER=riigiluup \
    //         -e POSTGRES_PASSWORD=riigiluup postgres:16-alpine
    //       set RIIGILUUP_IT_JDBC_URL=jdbc:postgresql://localhost:25432/riigiluup_it
    listOf("RIIGILUUP_IT_JDBC_URL", "RIIGILUUP_IT_JDBC_USER",
            "RIIGILUUP_IT_JDBC_PASSWORD", "DOCKER_HOST").forEach { key ->
        System.getenv(key)?.let { environment(key, it) }
    }
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}
