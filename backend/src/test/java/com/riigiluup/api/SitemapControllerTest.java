package com.riigiluup.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SitemapControllerTest {

    @Test
    void lastmodIsTheTallinnDayAndOmittedWhenUnknown() {
        StringBuilder sb = new StringBuilder();
        // 22:30 UTC on 30.09 is already 01.10 in Tallinn.
        SitemapController.loc(sb, "/votes/x", Instant.parse("2026-09-30T22:30:00Z"));
        SitemapController.loc(sb, "/initiatives/1", java.sql.Timestamp.from(Instant.parse("2026-05-02T10:00:00Z")));
        SitemapController.loc(sb, "/politicians/new-mp", null);
        assertThat(sb.toString())
                .contains("<loc>https://riigiluup.ee/votes/x</loc><lastmod>2026-10-01</lastmod>")
                .contains("<loc>https://riigiluup.ee/initiatives/1</loc><lastmod>2026-05-02</lastmod>")
                .contains("<loc>https://riigiluup.ee/politicians/new-mp</loc></url>");
    }
}
