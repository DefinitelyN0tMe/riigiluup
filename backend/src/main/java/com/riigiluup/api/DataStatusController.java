package com.riigiluup.api;

import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-source freshness for the витрина badges. Each dataset has its own refresh cadence, so the
 * badge on a page must reflect that page's job — members/legislation refresh once a day, votes
 * every 6 hours. Returning one row per relevant job lets the frontend pick the right one instead
 * of showing a single job's time with a mismatched cadence label.
 */
@RestController
@RequestMapping("/api/v1/data-status")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DataStatusController {

    private static final String SOURCE = "riigikogu";

    private record Tracked(String jobName, String cadence) {}

    private static final List<Tracked> TRACKED = List.of(
            new Tracked("plenary-members.full-refresh", "DAILY"),
            new Tracked("votes.window-refresh", "SIX_HOURLY"),
            new Tracked("legislation.window-refresh", "DAILY")
    );

    private final ImportRunLogRepository runLogRepo;

    @GetMapping
    public List<DataStatusDto> status() {
        List<DataStatusDto> out = new ArrayList<>();
        for (Tracked tr : TRACKED) {
            runLogRepo.findFirstBySourceNameAndJobNameOrderByStartedAtDesc(SOURCE, tr.jobName())
                    .ifPresent(log -> out.add(new DataStatusDto(
                            log.getSourceName(),
                            log.getJobName(),
                            log.getFinishedAt() != null ? log.getFinishedAt() : log.getStartedAt(),
                            log.getStatus(),
                            log.getRecordsUpserted(),
                            tr.cadence()
                    )));
        }
        return out;
    }
}
