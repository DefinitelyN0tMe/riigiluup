package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.question.GovernmentQuestion;
import com.riigiluup.question.GovernmentQuestionRepository;
import com.riigiluup.question.QuestionKind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Full refresh of interpellation / written-question volumes with government response
 * timing. The whole corpus (~4.4k rows since 2007) is ~23 list requests at size=200, so a
 * daily full pass is cheaper than tracking which open questions got answered. Each page is
 * upserted in its own transaction so the run never holds a connection across HTTP calls.
 */
@Slf4j
@Service
public class GovernmentQuestionImporter {

    private static final String JOB_NAME = "questions.full-refresh";
    private static final int PAGE_SIZE = 200;

    private final RiigikoguClient client;
    private final GovernmentQuestionRepository repo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;

    public GovernmentQuestionImporter(
            RiigikoguClient client,
            GovernmentQuestionRepository repo,
            ImportRunLogRepository runLogRepo,
            PlatformTransactionManager txManager
    ) {
        this.client = client;
        this.repo = repo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    /** Daily — answers land on open questions continuously; the pass is ~23 throttled calls. */
    @Scheduled(cron = "0 10 5 * * *", zone = "Europe/Tallinn")
    public void scheduledRefresh() {
        try {
            runFullRefresh();
        } catch (Exception e) {
            log.warn("Scheduled government-question refresh failed", e);
        }
    }

    public ImportRunLog runFullRefresh() {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(client.sourceName())
                .jobName(JOB_NAME)
                .startedAt(Instant.now())
                .status("RUNNING")
                .build());
        int seen = 0;
        int upserted = 0;
        try {
            seen += importKind(QuestionKind.INTERPELLATION);
            seen += importKind(QuestionKind.WRITTEN_QUESTION);
            upserted = seen;
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("government-question import failed", e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
        } finally {
            run.setRecordsSeen(seen);
            run.setRecordsUpserted(upserted);
            run.setFinishedAt(Instant.now());
            runLogRepo.save(run);
        }
        return run;
    }

    private int importKind(QuestionKind kind) {
        int imported = 0;
        int page = 0;
        int totalPages;
        do {
            QuestionListDto response = kind == QuestionKind.INTERPELLATION
                    ? client.fetchInterpellationsPage(page, PAGE_SIZE)
                    : client.fetchWrittenQuestionsPage(page, PAGE_SIZE);
            List<QuestionListDto.Item> items = response == null || response._embedded() == null
                    ? List.of() : response._embedded().content();
            totalPages = response == null || response.page() == null ? 0 : response.page().totalPages();
            final List<QuestionListDto.Item> pageItems = items;
            tx.executeWithoutResult(status -> upsertPage(kind, pageItems));
            imported += items.size();
            page++;
        } while (page < totalPages);
        log.info("imported {} {} volumes", imported, kind);
        return imported;
    }

    private void upsertPage(QuestionKind kind, List<QuestionListDto.Item> items) {
        for (QuestionListDto.Item item : items) {
            if (item.uuid() == null || item.submittingDate() == null) continue;
            GovernmentQuestion q = repo
                    .findBySourceNameAndExternalId(client.sourceName(), item.uuid())
                    .orElseGet(() -> GovernmentQuestion.builder()
                            .sourceName(client.sourceName())
                            .externalId(item.uuid())
                            .build());
            q.setKind(kind);
            q.setMark(item.mark());
            q.setMembership(item.membership());
            q.setTitle(item.title());
            String rawName = item.addressee() == null ? null : item.addressee().name();
            String rawRole = item.addressee() == null
                    ? null : GovernmentQuestionMapper.roleName(item.addressee().role());
            q.setAddresseeUuid(item.addressee() == null ? null : item.addressee().uuid());
            q.setAddresseeName(GovernmentQuestionMapper.personName(rawName, rawRole));
            q.setAddresseeRole(GovernmentQuestionMapper.roleOrName(rawRole, rawName));
            q.setSubmittingDate(LocalDate.parse(item.submittingDate()));
            q.setAnswerDeadline(item.answerDeadline() == null
                    ? null : LocalDate.parse(item.answerDeadline()));
            q.setAnsweredDate(GovernmentQuestionMapper.answeredDate(kind, item));
            q.setImportedAt(Instant.now());
            repo.save(q);
        }
    }
}
