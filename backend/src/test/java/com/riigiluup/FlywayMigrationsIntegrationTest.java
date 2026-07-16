package com.riigiluup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sanity-checks the Flyway migrations against a real PostgreSQL 16 container:
 *   1. Every migration script must apply cleanly (implicit — context wouldn't
 *      boot otherwise).
 *   2. Every domain table we depend on in the app must exist.
 */
class FlywayMigrationsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void all_expected_tables_exist() throws Exception {
        Set<String> tables = new LinkedHashSet<>();
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.getMetaData().getTables(
                     null, "public", "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }

        List<String> expected = List.of(
                "plenary_member",
                "group",
                "group_membership",
                "vote_event",
                "individual_vote",
                "legislative_item",
                "legislative_stage",
                "party",
                "faction_party_link",
                "topic",
                "import_run_log",
                "source_snapshot",
                "vote_faction_alignment"
        );
        assertThat(tables).as("Flyway-managed tables in public schema")
                .containsAll(expected);
    }

    @Test
    void flyway_schema_history_records_all_expected_versions() throws Exception {
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.createStatement().executeQuery(
                     "select version, success from flyway_schema_history " +
                             "order by installed_rank")) {
            Set<String> applied = new LinkedHashSet<>();
            while (rs.next()) {
                assertThat(rs.getBoolean("success"))
                        .as("migration success flag")
                        .isTrue();
                applied.add(rs.getString("version"));
            }
            assertThat(applied).contains("1", "2", "3", "4", "5", "6");
        }
    }
}
