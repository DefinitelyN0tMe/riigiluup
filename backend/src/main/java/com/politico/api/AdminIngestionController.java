package com.politico.api;

import com.politico.ingestion.riigikogu.ImportRunLog;
import com.politico.ingestion.riigikogu.PlenaryMemberDetailImporter;
import com.politico.ingestion.riigikogu.PlenaryMemberImporter;
import com.politico.ingestion.riigikogu.UsergroupImporter;
import com.politico.ingestion.riigikogu.VoteEventImporter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/import")
@RequiredArgsConstructor
public class AdminIngestionController {

    private final PlenaryMemberImporter memberImporter;
    private final UsergroupImporter usergroupImporter;
    private final PlenaryMemberDetailImporter detailImporter;
    private final VoteEventImporter voteImporter;

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
}
