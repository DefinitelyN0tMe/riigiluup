package com.riigiluup.api;

import com.riigiluup.activity.MemberActivityImporter;
import com.riigiluup.alignment.FactionAlignmentBackfillService;
import com.riigiluup.election.ElectionResultsImporter;
import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.LegislativeItemImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberDetailImporter;
import com.riigiluup.ingestion.riigikogu.PlenaryMemberImporter;
import com.riigiluup.ingestion.riigikogu.UsergroupImporter;
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
    private final ElectionResultsImporter electionResultsImporter;
    private final MemberActivityImporter memberActivityImporter;

    @PostMapping("/plenary-members")
    public ImportRunLog runPlenaryMembersImport() {
        return memberImporter.runOnce();
    }

    @PostMapping("/usergroups")
    public ImportRunLog runUsergroupsImport() {
        return usergroupImporter.runOnce();
    }

    @PostMapping("/plenary-member-details")
    public ImportRunLog runDetailImport() {
        return detailImporter.runOnce();
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
     * One-shot import of RK_2023 election results from opendata.valimised.ee,
     * matched to seated MPs by name. Immutable data — idempotent upsert.
     */
    @PostMapping("/elections")
    public Map<String, Object> runElectionResultsImport() {
        return Map.of("matched", electionResultsImporter.importRk2023());
    }

    /**
     * Recompute per-MP activity (speeches, questions, interpellations, written questions)
     * from the Riigikogu API into member_activity. A few hundred throttled calls (~minutes).
     */
    @PostMapping("/activity")
    public Map<String, Object> runActivityCompute() {
        return Map.of("computed", memberActivityImporter.computeAll());
    }
}
