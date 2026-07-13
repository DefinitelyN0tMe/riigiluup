package com.politico.api;

import com.politico.ingestion.riigikogu.ImportRunLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/data-status")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DataStatusController {

    private final ImportRunLogRepository runLogRepo;

    @GetMapping
    public List<DataStatusDto> status() {
        return runLogRepo
                .findFirstBySourceNameAndJobNameOrderByStartedAtDesc(
                        "riigikogu", "plenary-members.full-refresh")
                .map(log -> List.of(new DataStatusDto(
                        log.getSourceName(),
                        log.getJobName(),
                        log.getFinishedAt() != null
                                ? log.getFinishedAt()
                                : log.getStartedAt(),
                        log.getStatus(),
                        log.getRecordsUpserted()
                )))
                .orElse(List.of());
    }
}
