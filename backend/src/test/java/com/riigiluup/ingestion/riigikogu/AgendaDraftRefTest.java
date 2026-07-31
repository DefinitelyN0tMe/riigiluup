package com.riigiluup.ingestion.riigikogu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgendaDraftRefTest {

    @Test
    void parses_a_single_draft_code_from_a_reading_title() {
        List<AgendaDraftRef> refs = AgendaDraftRef.parse(
                "Konkurentsiseaduse muutmise seaduse eelnõu (609 SE) esimene lugemine");
        assertThat(refs).containsExactly(new AgendaDraftRef(609, "SE"));
    }

    @Test
    void parses_multiple_distinct_codes_from_a_combined_reading() {
        List<AgendaDraftRef> refs = AgendaDraftRef.parse(
                "Eelnõude (644 SE) ja (645 OE) teine lugemine");
        assertThat(refs).containsExactly(
                new AgendaDraftRef(644, "SE"),
                new AgendaDraftRef(645, "OE"));
    }

    @Test
    void deduplicates_a_code_repeated_in_the_title() {
        List<AgendaDraftRef> refs = AgendaDraftRef.parse("Arutelu (644 SE), jätkub (644 SE)");
        assertThat(refs).containsExactly(new AgendaDraftRef(644, "SE"));
    }

    @Test
    void tolerates_a_missing_space_inside_the_parentheses() {
        assertThat(AgendaDraftRef.parse("Eelnõu (644SE) lugemine"))
                .containsExactly(new AgendaDraftRef(644, "SE"));
    }

    @Test
    void yields_nothing_for_procedural_and_debate_items() {
        assertThat(AgendaDraftRef.parse("Istungi rakendamine")).isEmpty();
        assertThat(AgendaDraftRef.parse("Infotund")).isEmpty();
        assertThat(AgendaDraftRef.parse("Sisejulgeolek")).isEmpty();
        assertThat(AgendaDraftRef.parse(null)).isEmpty();
        assertThat(AgendaDraftRef.parse("")).isEmpty();
    }

    @Test
    void does_not_mistake_a_bare_year_or_letterless_parenthesis_for_a_code() {
        assertThat(AgendaDraftRef.parse("Riigieelarve (2025) arutelu")).isEmpty();
        assertThat(AgendaDraftRef.parse("Otsus (RES)")).isEmpty();
    }
}
