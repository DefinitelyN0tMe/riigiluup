package com.riigiluup.election;

import com.riigiluup.person.PlenaryMember;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ElectionResultsImporterTest {

    private static PlenaryMember member(String externalId, String first, String last, boolean active) {
        return PlenaryMember.builder()
                .externalId(externalId).firstName(first).lastName(last).active(active)
                .build();
    }

    @Test
    void name_key_is_case_insensitive_and_trimmed() {
        assertThat(ElectionResultsImporter.nameKey("Tarmo", "Tamm"))
                .isEqualTo(ElectionResultsImporter.nameKey("  TARMO ", "tamm"))
                .isEqualTo("tarmo|tamm");
    }

    @Test
    void active_member_wins_a_name_collision_regardless_of_input_order() {
        PlenaryMember active = member("ACTIVE", "Tarmo", "Tamm", true);
        PlenaryMember inactive = member("INACTIVE", "Tarmo", "Tamm", false);
        String key = ElectionResultsImporter.nameKey("Tarmo", "Tamm");

        Map<String, PlenaryMember> idxA = ElectionResultsImporter.activeWinsNameIndex(List.of(inactive, active));
        Map<String, PlenaryMember> idxB = ElectionResultsImporter.activeWinsNameIndex(List.of(active, inactive));

        assertThat(idxA.get(key).getExternalId()).isEqualTo("ACTIVE");
        assertThat(idxB.get(key).getExternalId()).isEqualTo("ACTIVE");
    }

    @Test
    void distinct_names_each_get_their_own_entry() {
        Map<String, PlenaryMember> idx = ElectionResultsImporter.activeWinsNameIndex(List.of(
                member("A", "Kaja", "Kallas", true),
                member("B", "Jüri", "Ratas", true)));

        assertThat(idx).hasSize(2);
        assertThat(idx.get(ElectionResultsImporter.nameKey("Kaja", "Kallas")).getExternalId()).isEqualTo("A");
        assertThat(idx.get(ElectionResultsImporter.nameKey("Jüri", "Ratas")).getExternalId()).isEqualTo("B");
    }
}
