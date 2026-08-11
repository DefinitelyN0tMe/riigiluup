package com.riigiluup.api;

import com.riigiluup.activity.MemberActivityImporter;
import com.riigiluup.alignment.FactionAlignmentBackfillService;
import com.riigiluup.finance.PartyFinanceImporter;
import com.riigiluup.election.ElectionResultsImporter;
import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.LegislativeItemImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberDetailImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberImporter;
import com.riigiluup.ingestion.riigikogu.UsergroupImporter;
import com.riigiluup.ingestion.riigikogu.SpeechBillLinker;
import com.riigiluup.ingestion.riigikogu.SponsorRelinker;
import com.riigiluup.ingestion.riigikogu.VoteBillLinker;
import com.riigiluup.ingestion.riigikogu.VoteEventImporter;
import com.riigiluup.ingestion.wikidata.WikidataImporter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/import")
@RequiredArgsConstructor
public class AdminIngestionController {

    private final PlenaryMemberImporter memberImporter;
    private final UsergroupImporter usergroupImporter;
    private final PlenaryMemberDetailImporter detailImporter;
    private final VoteEventImporter voteImporter;
    private final FactionAlignmentBackfillService alignmentBackfill;
    private final LegislativeItemImporter legislationImporter;
    private final VoteBillLinker voteBillLinker;
    private final WikidataImporter wikidataImporter;
    private final SponsorRelinker sponsorRelinker;
    private final SpeechBillLinker speechBillLinker;
    private final ElectionResultsImporter electionResultsImporter;
    private final com.riigiluup.election.HistoricalElectionImporter historicalElectionImporter;
    private final MemberActivityImporter memberActivityImporter;
    private final PartyFinanceImporter partyFinanceImporter;
    private final com.riigiluup.ingestion.riigikogu.GovernmentQuestionImporter governmentQuestionImporter;
    private final com.riigiluup.ingestion.riigiteataja.RtLinker rtLinker;
    private final com.riigiluup.ingestion.rahvaalgatus.RahvaalgatusImporter rahvaalgatusImporter;

    @PostMapping("/plenary-members")
    public ImportRunLog runPlenaryMembersImport() {
        return memberImporter.runOnce();
    }

    @PostMapping("/usergroups")
    public ImportRunLog runUsergroupsImport() {
        return usergroupImporter.runOnce();
    }

    /**
     * Refresh MP detail (bio, photo, committees, faction) + faction history. {@code force=true}
     * bypasses the 7-day freshness window to pick up a just-happened change (e.g. a faction
     * departure) or to rebuild the faction-history timeline immediately.
     */
    @PostMapping("/plenary-member-details")
    public ImportRunLog runDetailImport(@RequestParam(defaultValue = "false") boolean force) {
        return detailImporter.runOnce(force);
    }

    @PostMapping("/votes")
    public ImportRunLog runVotesImport(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        LocalDate today = LocalDate.now();
        LocalDate effectiveTo = to != null ? to : today;
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(90);
        return voteImporter.runWindow(effectiveFrom, effectiveTo);
    }

    @PostMapping("/recompute-alignments")
    public Map<String, Object> recomputeAlignments() {
        int processed = alignmentBackfill.recomputeAll();
        return Map.of("processedEvents", processed);
    }

    @PostMapping("/legislation")
    public ImportRunLog runLegislationImport(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        LocalDate today = LocalDate.now();
        LocalDate effectiveTo = to != null ? to : today;
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(90);
        return legislationImporter.runWindow(effectiveFrom, effectiveTo);
    }

    @PostMapping("/link-votes-to-bills")
    public Map<String, Object> linkVotesToBills() {
        return Map.of("linked", voteBillLinker.linkAll());
    }

    /**
     * One-shot Wikidata cross-reference — populates wikidata_qid + wikipedia_url_*
     * on plenary_member by matching (fullName, dateOfBirth). Safe to re-run.
     */
    @PostMapping("/wikidata")
    public ImportRunLog runWikidataCrossref() {
        return wikidataImporter.runOnce();
    }

    /**
     * Back-fill plenary_member_id on legislative_sponsorship rows that were stored
     * with a NULL FK because the sponsor UUID wasn't yet in plenary_member at bill
     * ingest time. Idempotent: only touches NULL rows.
     */
    @PostMapping("/relink-sponsors")
    public Map<String, Object> relinkSponsors() {
        return Map.of("relinked", sponsorRelinker.relinkOrphanSponsors());
    }

    /**
     * Rebuild speech -> bill links from agenda-item titles for speeches already ingested.
     * Idempotent full rebuild; ongoing speech imports write these links per sitting.
     */
    @PostMapping("/link-speeches-to-bills")
    public Map<String, Object> linkSpeechesToBills() {
        return speechBillLinker.linkAll();
    }

    /**
     * Import the MP electoral footprint from opendata.valimised.ee: the RK_2023 seat plus the
     * EP/KOV campaigns, matched to MPs by name. Immutable data — idempotent upsert per election.
     */
    @PostMapping("/elections")
    public Map<String, Object> runElectionResultsImport() {
        int rk = electionResultsImporter.importRk2023();
        Map<String, Integer> campaigns = electionResultsImporter.importCampaigns();
        return Map.of("RK_2023", rk, "campaigns", campaigns);
    }

    /**
     * Import the pre-2023 Riigikogu electoral history for current MPs from Martin Mölder's
     * compiled dataset (bundled, 1992-2019). Matched by birth date + surname token; idempotent
     * full replace of the historical rows.
     */
    @PostMapping("/elections-historical")
    public Map<String, Object> runHistoricalElectionImport() {
        return Map.of("historicalRows", historicalElectionImporter.importHistorical());
    }

    /**
     * Recompute per-MP activity (speeches, questions, interpellations, written questions)
     * from the Riigikogu API into member_activity. A few hundred throttled calls (~minutes).
     */
    @PostMapping("/activity")
    public Map<String, Object> runActivityCompute() {
        return Map.of("computed", memberActivityImporter.computeAll());
    }

    /** Full-replace import of ERJK party income data (money in politics). */
    @PostMapping("/party-finance")
    public Map<String, Object> runPartyFinanceImport() {
        return Map.of("rows", partyFinanceImporter.importAll());
    }

    /**
     * Full refresh of interpellation / written-question volumes with response dates
     * (~23 throttled list calls for the whole corpus since 2007).
     */
    @PostMapping("/questions")
    public ImportRunLog runQuestionsImport() {
        return governmentQuestionImporter.runFullRefresh();
    }

    /**
     * Link adopted laws to Riigi Teataja (1 throttled RT call per candidate, newest
     * first). The historical backlog is ~3.8k candidates — run in slices.
     */
    @PostMapping("/rt-links")
    public Map<String, Integer> runRtLinking(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) LocalDate since) {
        return rtLinker.linkBatch(Math.min(limit, 1000), since);
    }

    @PostMapping("/initiatives")
    public ImportRunLog runInitiativesImport() {
        return rahvaalgatusImporter.runFullRefresh();
    }
}
