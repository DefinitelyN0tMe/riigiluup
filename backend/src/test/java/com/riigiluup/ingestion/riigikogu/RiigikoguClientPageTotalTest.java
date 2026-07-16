package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the HAL PagedModel count extraction used for interpellation / written-question
 * totals — the count lives under {@code page.totalElements}, not at the top level.
 */
class RiigikoguClientPageTotalTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String s) throws Exception {
        return mapper.readTree(s);
    }

    @Test
    void reads_total_from_hal_page_block() throws Exception {
        assertThat(RiigikoguClient.pageTotal(json("""
                {"_embedded":{"content":[]},"page":{"size":1,"totalElements":42,"totalPages":42,"number":0}}
                """))).isEqualTo(42);
    }

    @Test
    void falls_back_to_top_level_total_elements() throws Exception {
        assertThat(RiigikoguClient.pageTotal(json("{\"totalElements\":7}"))).isEqualTo(7);
    }

    @Test
    void returns_zero_when_absent_or_null() throws Exception {
        assertThat(RiigikoguClient.pageTotal(json("{}"))).isZero();
        assertThat(RiigikoguClient.pageTotal(null)).isZero();
    }
}
