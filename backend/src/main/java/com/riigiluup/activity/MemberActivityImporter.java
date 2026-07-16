package com.riigiluup.activity;

import com.riigiluup.ingestion.riigikogu.RiigikoguClient;
import com.riigiluup.ingestion.riigikogu.SpeechCountDto;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.statistics.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Recomputes per-MP activity (speeches, questions, interpellations, written questions)
 * from the Riigikogu API and caches it in member_activity. Speech/question tallies come
 * in a single call (all UUIDs); interpellation and written-question counts are one call
 * per MP, so the whole pass is a few hundred throttled requests — run weekly, not on
 * every request. Not wrapped in one transaction so it never holds a DB connection across
 * the slow HTTP fan-out; each MP's row is saved independently.
 */
@Service
public class MemberActivityImporter {

    private static final Logger log = LoggerFactory.getLogger(MemberActivityImporter.class);

    private final RiigikoguClient client;
    private final PlenaryMemberRepository memberRepo;
    private final MemberActivityRepository repo;

    public MemberActivityImporter(RiigikoguClient client,
                                  PlenaryMemberRepository memberRepo,
                                  MemberActivityRepository repo) {
        this.client = client;
        this.memberRepo = memberRepo;
        this.repo = repo;
    }

    /** Weekly refresh — activity accrues slowly and the pass is a few hundred API calls. */
    @Scheduled(cron = "0 30 5 ? * SUN", zone = "Europe/Tallinn")
    public void scheduledCompute() {
        try {
            computeAll();
        } catch (Exception e) {
            log.warn("Scheduled MP activity refresh failed", e);
        }
    }

    public int computeAll() {
        List<PlenaryMember> active = memberRepo.findAll().stream()
                .filter(PlenaryMember::isActive)
                .toList();
        if (active.isEmpty()) return 0;

        List<String> uuids = active.stream().map(PlenaryMember::getExternalId).toList();
        LocalDate today = LocalDate.now();
        Map<String, SpeechCountDto> speechByUuid = client
                .fetchSpeechCounts(uuids, StatisticsService.TERM_START, today).stream()
                .collect(Collectors.toMap(SpeechCountDto::uuid, Function.identity(), (a, b) -> a));

        Instant now = Instant.now();
        int n = 0;
        for (PlenaryMember m : active) {
            SpeechCountDto s = speechByUuid.get(m.getExternalId());
            int interpellations = client.countInterpellations(m.getExternalId());
            int writtenQuestions = client.countWrittenQuestions(m.getExternalId());

            MemberActivity a = repo.findById(m.getExternalId()).orElseGet(MemberActivity::new);
            a.setMemberExternalId(m.getExternalId());
            a.setSpeeches(s == null ? 0 : s.speeches());
            a.setQuestions(s == null ? 0 : s.questions());
            a.setInterpellations(interpellations);
            a.setWrittenQuestions(writtenQuestions);
            a.setComputedAt(now);
            repo.save(a);
            n++;
        }
        log.info("Computed parliamentary activity for {} active MPs", n);
        return n;
    }
}
