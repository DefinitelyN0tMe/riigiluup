package com.riigiluup.api;

import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import com.riigiluup.legislation.LegislationPhase;
import com.riigiluup.legislation.LegislativeItemRepository;
import com.riigiluup.vote.VoteEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Public, read-only figures for the homepage stat strip. Kept deliberately cheap (a couple of
 * indexed counts + two small zero-filled time series) and cached briefly at the edge.
 */
@RestController
@RequestMapping("/api/v1/home")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HomeSummaryController {

    private static final ZoneId TALLINN = ZoneId.of("Europe/Tallinn");
    private static final List<LegislationPhase> IN_PROGRESS = List.of(
            LegislationPhase.SUBMITTED, LegislationPhase.IN_COMMITTEE, LegislationPhase.IN_READINGS);

    private final ImportRunLogRepository runLogRepo;
    private final VoteEventRepository voteRepo;
    private final LegislativeItemRepository itemRepo;

    @PersistenceContext
    private EntityManager em;

    @Value("${riigiluup.schedule.votes-refresh-cron}")
    private String votesCron;

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public HomeSummaryDto summary() {
        Instant lastSync = runLogRepo.findLastSuccessfulSyncAt().orElse(null);
        Instant nextSync = nextCronFire(votesCron);
        long votesThisWeek = voteRepo.countByStartedAtAfter(Instant.now().minus(7, ChronoUnit.DAYS));
        long billsInProgress = itemRepo.countByPhaseIn(IN_PROGRESS);

        // Zero-filled so a sparse (recess) window renders as a flat line, not a gap.
        List<Integer> votesPerDay = intSeries("""
                SELECT COUNT(v.id)::int
                FROM generate_series(
                       date_trunc('day', now() AT TIME ZONE 'Europe/Tallinn') - interval '13 days',
                       date_trunc('day', now() AT TIME ZONE 'Europe/Tallinn'),
                       interval '1 day') AS d
                LEFT JOIN vote_event v
                  ON date_trunc('day', v.started_at AT TIME ZONE 'Europe/Tallinn') = d
                GROUP BY d ORDER BY d
                """);
        List<Integer> billsPerWeek = intSeries("""
                SELECT COUNT(i.id)::int
                FROM generate_series(
                       date_trunc('week', now() AT TIME ZONE 'Europe/Tallinn') - interval '11 weeks',
                       date_trunc('week', now() AT TIME ZONE 'Europe/Tallinn'),
                       interval '1 week') AS w
                LEFT JOIN legislative_item i
                  ON date_trunc('week', i.initiated_date::timestamp) = w
                GROUP BY w ORDER BY w
                """);

        return new HomeSummaryDto(lastSync, nextSync, votesThisWeek, billsInProgress,
                votesPerDay, billsPerWeek);
    }

    private List<Integer> intSeries(String sql) {
        List<?> rows = em.createNativeQuery(sql).getResultList();
        List<Integer> out = new ArrayList<>(rows.size());
        for (Object o : rows) out.add(((Number) o).intValue());
        return out;
    }

    private static Instant nextCronFire(String cron) {
        try {
            ZonedDateTime next = CronExpression.parse(cron).next(ZonedDateTime.now(TALLINN));
            return next == null ? null : next.toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}
