package com.politico.admin;

import com.politico.group.GroupRepository;
import com.politico.ingestion.riigikogu.ImportRunLogRepository;
import com.politico.legislation.LegislativeItemRepository;
import com.politico.person.PlenaryMemberRepository;
import com.politico.source.SourceSnapshotRepository;
import com.politico.vote.IndividualVoteRepository;
import com.politico.vote.VoteEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/status")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminStatusController {

    private final ImportRunLogRepository runLogRepo;
    private final PlenaryMemberRepository memberRepo;
    private final GroupRepository groupRepo;
    private final VoteEventRepository voteEventRepo;
    private final IndividualVoteRepository individualVoteRepo;
    private final LegislativeItemRepository itemRepo;
    private final SourceSnapshotRepository snapshotRepo;

    @PersistenceContext
    private EntityManager em;

    @GetMapping
    public AdminStatusDto status() {
        var runs = runLogRepo.findAll(PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "startedAt")));
        List<AdminStatusDto.JobStatus> jobs = new ArrayList<>();
        for (var r : runs) {
            jobs.add(new AdminStatusDto.JobStatus(
                    r.getSourceName(), r.getJobName(),
                    r.getStatus(),
                    r.getFinishedAt() != null ? r.getFinishedAt() : r.getStartedAt(),
                    r.getRecordsSeen(), r.getRecordsUpserted(), r.getErrorMessage()));
        }

        List<Tuple> snapRows = em.createQuery(
                "select s.entityType, count(s) from SourceSnapshot s group by s.entityType",
                Tuple.class).getResultList();
        List<AdminStatusDto.SnapshotSummary> summaries = new ArrayList<>();
        for (Tuple t : snapRows) {
            summaries.add(new AdminStatusDto.SnapshotSummary(
                    t.get(0, String.class),
                    t.get(1, Long.class)));
        }

        AdminStatusDto.DomainCounts counts = new AdminStatusDto.DomainCounts(
                memberRepo.count(),
                groupRepo.count(),
                voteEventRepo.count(),
                individualVoteRepo.count(),
                itemRepo.count()
        );

        return new AdminStatusDto(jobs, summaries, counts, Instant.now());
    }
}
