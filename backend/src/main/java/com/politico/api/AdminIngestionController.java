package com.politico.api;

import com.politico.ingestion.riigikogu.ImportRunLog;
import com.politico.ingestion.riigikogu.PlenaryMemberImporter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/import")
@RequiredArgsConstructor
public class AdminIngestionController {

    private final PlenaryMemberImporter importer;

    // Phase 1: unprotected. Basic auth or IP allow-list is Phase 6.
    @PostMapping("/plenary-members")
    public ImportRunLog runPlenaryMembersImport() {
        return importer.runOnce();
    }
}
