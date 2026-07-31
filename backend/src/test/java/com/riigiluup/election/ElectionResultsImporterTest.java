package com.riigiluup.election;

import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    /** An empty RESULTS.xml must never wipe the table — the run fails, existing rows stay. */
    @Test
    void empty_source_result_keeps_existing_rows_and_marks_run_failed() {
        ElectionResultsClient client = mock(ElectionResultsClient.class);
        ElectionResultRepository repo = mock(ElectionResultRepository.class);
        ImportRunLogRepository runLogRepo = mock(ImportRunLogRepository.class);
        when(runLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(client.fetchRk2023Results()).thenReturn(List.of());

        ElectionResultsImporter importer = new ElectionResultsImporter(
                client, mock(PlenaryMemberRepository.class), repo, runLogRepo,
                mock(PlatformTransactionManager.class));

        assertThatThrownBy(importer::importRk2023).isInstanceOf(IllegalStateException.class);

        verify(repo, never()).deleteByElectionCode(anyString());
        ArgumentCaptor<ImportRunLog> captor = ArgumentCaptor.forClass(ImportRunLog.class);
        verify(runLogRepo, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getFinishedAt()).isNotNull();
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

    private static ElectionCandidateDto cand(String first, String last, boolean elected) {
        return new ElectionCandidateDto(first, last, 100, 1, elected ? "PERSONAL" : null,
                "Party", "P", 10, elected);
    }

    @Test
    void campaign_match_keeps_only_names_unique_on_both_sides() {
        List<PlenaryMember> roster = List.of(
                member("KAJA", "Kaja", "Kallas", true),
                member("JYRI", "Jüri", "Ratas", true));
        List<ElectionCandidateDto> candidates = List.of(
                cand("Kaja", "Kallas", true),   // unique both sides -> matched
                cand("Keegi", "Tundmatu", false)); // not an MP -> ignored

        List<ElectionResultsImporter.CampaignMatch> matches =
                ElectionResultsImporter.matchCampaign(roster, candidates);

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).member().getExternalId()).isEqualTo("KAJA");
        assertThat(matches.get(0).candidate().surname()).isEqualTo("Kallas");
    }

    @Test
    void campaign_match_skips_a_name_shared_by_two_candidates() {
        List<PlenaryMember> roster = List.of(member("MP", "Jaan", "Tamm", true));
        List<ElectionCandidateDto> candidates = List.of(
                cand("Jaan", "Tamm", false),
                cand("Jaan", "Tamm", true)); // two same-name candidates -> cannot disambiguate

        assertThat(ElectionResultsImporter.matchCampaign(roster, candidates)).isEmpty();
    }

    @Test
    void campaign_match_skips_a_name_shared_by_two_roster_members() {
        List<PlenaryMember> roster = List.of(
                member("A", "Jaan", "Tamm", true),
                member("B", "Jaan", "Tamm", false));
        List<ElectionCandidateDto> candidates = List.of(cand("Jaan", "Tamm", true));

        assertThat(ElectionResultsImporter.matchCampaign(roster, candidates)).isEmpty();
    }
}
