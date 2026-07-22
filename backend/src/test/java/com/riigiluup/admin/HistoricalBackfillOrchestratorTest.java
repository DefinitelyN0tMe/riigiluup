package com.riigiluup.admin;

import com.riigiluup.activity.MemberActivityImporter;
import com.riigiluup.election.ElectionResultsImporter;
import com.riigiluup.finance.PartyFinanceImporter;
import com.riigiluup.ingestion.rahvaalgatus.RahvaalgatusImporter;
import com.riigiluup.ingestion.riigikogu.GovernmentQuestionImporter;
import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.LegislativeItemImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberDetailImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberImporter;
import com.riigiluup.ingestion.riigikogu.SpeechImporter;
import com.riigiluup.ingestion.riigikogu.SponsorRelinker;
import com.riigiluup.ingestion.riigikogu.UsergroupImporter;
import com.riigiluup.ingestion.riigikogu.VoteBillLinker;
import com.riigiluup.ingestion.riigikogu.VoteEventImporter;
import com.riigiluup.ingestion.riigiteataja.RtLinker;
import com.riigiluup.ingestion.wikidata.WikidataImporter;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drives {@link HistoricalBackfillOrchestrator#runLoop} synchronously with every importer
 * mocked, asserting the dependency-ordered sequence, kind-gating, and terminal status. The
 * async executor is bypassed so there is no timing flakiness.
 */
class HistoricalBackfillOrchestratorTest {

    private final BackfillRunRepository runRepo = mock(BackfillRunRepository.class);
    private final PlenaryMemberImporter memberImporter = mock(PlenaryMemberImporter.class);
    private final UsergroupImporter usergroupImporter = mock(UsergroupImporter.class);
    private final PlenaryMemberDetailImporter detailImporter = mock(PlenaryMemberDetailImporter.class);
    private final ElectionResultsImporter electionResultsImporter = mock(ElectionResultsImporter.class);
    private final WikidataImporter wikidataImporter = mock(WikidataImporter.class);
    private final LegislativeItemImporter legislationImporter = mock(LegislativeItemImporter.class);
    private final VoteEventImporter voteImporter = mock(VoteEventImporter.class);
    private final SpeechImporter speechImporter = mock(SpeechImporter.class);
    private final GovernmentQuestionImporter governmentQuestionImporter = mock(GovernmentQuestionImporter.class);
    private final RahvaalgatusImporter rahvaalgatusImporter = mock(RahvaalgatusImporter.class);
    private final PartyFinanceImporter partyFinanceImporter = mock(PartyFinanceImporter.class);
    private final MemberActivityImporter memberActivityImporter = mock(MemberActivityImporter.class);
    private final VoteBillLinker voteBillLinker = mock(VoteBillLinker.class);
    private final SponsorRelinker sponsorRelinker = mock(SponsorRelinker.class);
    private final RtLinker rtLinker = mock(RtLinker.class);
    private final com.riigiluup.common.AnalyticsCacheEvictor cacheEvictor =
            mock(com.riigiluup.common.AnalyticsCacheEvictor.class);

    private final HistoricalBackfillOrchestrator orchestrator = new HistoricalBackfillOrchestrator(
            runRepo, memberImporter, usergroupImporter, detailImporter, electionResultsImporter,
            wikidataImporter, legislationImporter, voteImporter, speechImporter,
            governmentQuestionImporter, rahvaalgatusImporter, partyFinanceImporter,
            memberActivityImporter, voteBillLinker, sponsorRelinker, rtLinker, cacheEvictor);

    private static ImportRunLog ok(int upserted) {
        return ImportRunLog.builder().status("SUCCESS").recordsUpserted(upserted).build();
    }

    /** A single BackfillRun the mock repo hands back on every findById, mutated in place. */
    private BackfillRun seedRun(int windowsTotal) {
        BackfillRun run = BackfillRun.builder()
                .id(UUID.randomUUID())
                .startedAt(Instant.now())
                .fromDate(LocalDate.of(2024, 1, 1))
                .toDate(LocalDate.of(2024, 1, 1))
                .currentWindowStart(LocalDate.of(2024, 1, 1))
                .kinds("ALL")
                .status(HistoricalBackfillOrchestrator.STATUS_RUNNING)
                .billsImported(0).votesImported(0)
                .windowsCompleted(0).windowsTotal(windowsTotal)
                .version(0L)
                .build();
        when(runRepo.findById(any())).thenReturn(Optional.of(run));
        when(runRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return run;
    }

    private void stubHappyPath() {
        when(memberImporter.runOnce()).thenReturn(ok(101));
        when(usergroupImporter.runOnce()).thenReturn(ok(340));
        when(detailImporter.runOnce()).thenReturn(ok(101));
        when(electionResultsImporter.importRk2023()).thenReturn(92);
        when(wikidataImporter.runOnce()).thenReturn(ok(194));
        when(legislationImporter.runAllDrafts()).thenReturn(ok(1450));
        when(voteImporter.runWindow(any(), any())).thenReturn(ok(210));
        when(speechImporter.runWindow(any(), any())).thenReturn(ok(2930));
        when(governmentQuestionImporter.runFullRefresh()).thenReturn(ok(4429));
        when(rahvaalgatusImporter.runFullRefresh()).thenReturn(ok(1141));
        when(partyFinanceImporter.importAll()).thenReturn(300);
        when(memberActivityImporter.computeAll()).thenReturn(101);
        when(voteBillLinker.linkAll()).thenReturn(46);
        when(sponsorRelinker.relinkOrphanSponsors()).thenReturn(3);
        when(rtLinker.linkBatch(anyInt(), any())).thenReturn(Map.of("candidates", 0, "linked", 0, "unmatched", 0));
    }

    @Test
    void all_runs_every_importer_in_dependency_order_and_completes() {
        BackfillRun run = seedRun(1);
        stubHappyPath();

        orchestrator.runLoop(run.getId(), run.getFromDate(), run.getToDate(),
                Set.copyOf(HistoricalBackfillOrchestrator.ALL_KINDS));

        InOrder o = inOrder(memberImporter, usergroupImporter, detailImporter,
                electionResultsImporter, wikidataImporter, legislationImporter,
                voteImporter, speechImporter, governmentQuestionImporter, rahvaalgatusImporter,
                partyFinanceImporter, memberActivityImporter, voteBillLinker, sponsorRelinker, rtLinker);
        o.verify(memberImporter).runOnce();
        o.verify(usergroupImporter).runOnce();
        o.verify(detailImporter).runOnce();
        o.verify(electionResultsImporter).importRk2023();
        o.verify(wikidataImporter).runOnce();
        o.verify(legislationImporter).runAllDrafts();
        o.verify(voteImporter).runWindow(any(), any());
        o.verify(speechImporter).runWindow(any(), any());
        o.verify(governmentQuestionImporter).runFullRefresh();
        o.verify(rahvaalgatusImporter).runFullRefresh();
        o.verify(partyFinanceImporter).importAll();
        o.verify(memberActivityImporter).computeAll();
        o.verify(voteBillLinker).linkAll();
        o.verify(sponsorRelinker).relinkOrphanSponsors();
        o.verify(rtLinker).linkBatch(anyInt(), any());

        assertThat(run.getStatus()).isEqualTo(HistoricalBackfillOrchestrator.STATUS_COMPLETED);
        assertThat(run.getPhase()).isEqualTo("completed");
        assertThat(run.getBillsImported()).isEqualTo(1450);
        assertThat(run.getVotesImported()).isEqualTo(210);
        assertThat(run.getStepCounts()).contains("\"members\":101").contains("\"rtLinks\":0");
    }

    @Test
    void subset_skips_unrequested_importers_and_their_linkers() {
        BackfillRun run = seedRun(1);
        when(memberImporter.runOnce()).thenReturn(ok(101));
        when(voteImporter.runWindow(any(), any())).thenReturn(ok(210));

        // Only members + votes. No bills → no vote-bill link, no sponsor relink; no wikidata etc.
        orchestrator.runLoop(run.getId(), run.getFromDate(), run.getToDate(),
                Set.of("MEMBERS", "VOTES"));

        verify(memberImporter).runOnce();
        verify(voteImporter).runWindow(any(), any());
        verify(wikidataImporter, never()).runOnce();
        verify(legislationImporter, never()).runAllDrafts();
        verify(speechImporter, never()).runWindow(any(), any());
        verify(governmentQuestionImporter, never()).runFullRefresh();
        verify(voteBillLinker, never()).linkAll();
        verify(sponsorRelinker, never()).relinkOrphanSponsors();
        verify(rtLinker, never()).linkBatch(anyInt(), any());
        assertThat(run.getStatus()).isEqualTo(HistoricalBackfillOrchestrator.STATUS_COMPLETED);
    }

    @Test
    void rt_link_drain_loops_until_a_batch_makes_no_progress() {
        BackfillRun run = seedRun(0);
        // First two batches link rows, third links nothing → drain stops after 3 calls.
        when(rtLinker.linkBatch(anyInt(), any()))
                .thenReturn(Map.of("candidates", 500, "linked", 500, "unmatched", 0))
                .thenReturn(Map.of("candidates", 500, "linked", 120, "unmatched", 380))
                .thenReturn(Map.of("candidates", 380, "linked", 0, "unmatched", 380));

        orchestrator.runLoop(run.getId(), run.getFromDate(), run.getToDate(), Set.of("RT_LINKS"));

        verify(rtLinker, org.mockito.Mockito.times(3)).linkBatch(anyInt(), any());
        assertThat(run.getStepCounts()).contains("\"rtLinks\":620");
        assertThat(run.getStatus()).isEqualTo(HistoricalBackfillOrchestrator.STATUS_COMPLETED);
    }

    @Test
    void canonicalize_expands_all_in_pipeline_order() {
        assertThat(HistoricalBackfillOrchestrator.canonicalize(Set.of("ALL")))
                .containsExactlyElementsOf(HistoricalBackfillOrchestrator.ALL_KINDS);
    }

    @Test
    void canonicalize_projects_onto_pipeline_order_regardless_of_input_order() {
        assertThat(HistoricalBackfillOrchestrator.canonicalize(Set.of("VOTES", "MEMBERS", "BILLS")))
                .containsExactly("MEMBERS", "BILLS", "VOTES");
    }

    @Test
    void canonicalize_drops_unknowns_and_defaults_empty_to_bills_votes() {
        assertThat(HistoricalBackfillOrchestrator.canonicalize(Set.of("NONSENSE")))
                .containsExactly("BILLS", "VOTES");
        assertThat(HistoricalBackfillOrchestrator.canonicalize(Set.of()))
                .containsExactly("BILLS", "VOTES");
    }
}
