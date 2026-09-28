package com.riigiluup.ingestion.schedule;

import com.riigiluup.alert.TelegramAlertService;
import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Independent safety net for every scheduled import. Importers record a failure as a FAILED row in
 * import_run_log and return normally, so a job can stay broken for days without an exception ever
 * reaching an alert (the bill import failed seven nights in a row in September 2026 unnoticed).
 * Twice a day this checks, for each job, that it landed data (SUCCESS or PARTIAL) within about twice
 * its schedule, and sends one Telegram message listing every job that did not. Never throws.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportFreshnessWatchdog {

    /** Job name -> the longest acceptable time since its last run that landed data. */
    static final Map<String, Duration> MAX_AGE = maxAges();

    private static Map<String, Duration> maxAges() {
        Map<String, Duration> m = new LinkedHashMap<>();
        m.put("votes.window-refresh", Duration.ofHours(13));          // every 6 h
        m.put("speeches.window-refresh", Duration.ofHours(13));       // every 6 h
        m.put("plenary-members.full-refresh", Duration.ofHours(30));  // nightly
        m.put("plenary-members.detail-refresh", Duration.ofHours(30));
        m.put("usergroups.full-refresh", Duration.ofHours(30));
        m.put("legislation.window-refresh", Duration.ofHours(30));
        m.put("oversight.refresh", Duration.ofHours(30));
        m.put("questions.full-refresh", Duration.ofHours(30));
        m.put("initiatives.full-refresh", Duration.ofHours(30));
        m.put("wikidata.mp-crossref", Duration.ofHours(74));          // every 2 days
        m.put("member-activity.refresh", Duration.ofDays(8));         // weekly
        m.put("party-finance.full-refresh", Duration.ofDays(35));     // monthly
        return m;
    }

    private static final Set<String> LANDED = Set.of("SUCCESS", "PARTIAL");

    private final ImportRunLogRepository runLogRepo;
    private final TelegramAlertService alert;
    private final Clock clock = Clock.systemUTC();

    @Scheduled(cron = "0 30 7,19 * * *", zone = "Europe/Tallinn")
    public void check() {
        try {
            List<String> problems = findProblems(Instant.now(clock));
            if (problems.isEmpty()) {
                log.info("import freshness ok: {} jobs within their schedule", MAX_AGE.size());
                return;
            }
            String msg = "⚠️ RiigiLuup: andmed ei ole värsked\n" + String.join("\n", problems);
            log.warn(msg);
            alert.send(msg);
        } catch (Exception e) {
            log.warn("import freshness check failed to run: {}", e.toString());
        }
    }

    List<String> findProblems(Instant now) {
        List<String> problems = new ArrayList<>();
        MAX_AGE.forEach((job, maxAge) -> {
            Optional<ImportRunLog> landed = runLogRepo.findFirstByJobNameAndStatusInOrderByStartedAtDesc(job, LANDED);
            Instant last = landed.map(ImportRunLog::getStartedAt).orElse(null);
            if (last != null && Duration.between(last, now).compareTo(maxAge) <= 0) return;
            String lastErr = runLogRepo.findFirstByJobNameOrderByStartedAtDesc(job)
                    .filter(r -> !LANDED.contains(r.getStatus()))
                    .map(r -> " (viimane: " + r.getStatus()
                            + (r.getErrorMessage() != null ? ", " + abbreviate(r.getErrorMessage()) : "") + ")")
                    .orElse("");
            problems.add("• " + job + ": " + (last == null ? "pole kordagi õnnestunud"
                    : "viimati õnnestus " + Duration.between(last, now).toHours() + " h tagasi") + lastErr);
        });
        return problems;
    }

    private static String abbreviate(String s) {
        String one = s.replaceAll("\\s+", " ").trim();
        return one.length() > 140 ? one.substring(0, 140) + "…" : one;
    }
}
