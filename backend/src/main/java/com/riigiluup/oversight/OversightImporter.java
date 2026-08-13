package com.riigiluup.oversight;

import com.fasterxml.jackson.databind.JsonNode;
import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import com.riigiluup.ingestion.riigikogu.RiigikoguClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ingests parliamentary oversight (written questions + interpellations) from the Riigikogu document
 * register and links each question to its answer via the shared volume. Two entry points: a one-time
 * {@link #backfillCurrentTerm()} (all XV-term questions/answers) and a cheap daily
 * {@link #refreshRecent()} (new questions in a short window + new answers in a wider one, so an answer
 * to an older question is still picked up). Detail fetches are throttled by the client.
 */
@Slf4j
@Service
public class OversightImporter {

    private static final String JOB_REFRESH = "oversight.refresh";
    private static final String JOB_BACKFILL = "oversight.backfill";
    /** XV Riigikogu convened 2023-04-10; a small buffer guards boundary documents. */
    private static final LocalDate TERM_START = LocalDate.of(2023, 3, 1);
    private static final int PAGE_SIZE = 100;
    private static final int SAFETY_PAGES = 400;

    private static final String T_WRITTEN_Q = "writtenQuestionDocument";
    private static final String T_WRITTEN_A = "writtenQuestionAnswerDocument";
    private static final String T_INTERP_Q = "interpellationsDocument";
    private static final String T_INTERP_A = "interpellationsAnswerDocument";

    private final RiigikoguClient client;
    private final OversightItemRepository itemRepo;
    private final OversightEnquirerRepository enquirerRepo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;

    public OversightImporter(RiigikoguClient client,
                             OversightItemRepository itemRepo,
                             OversightEnquirerRepository enquirerRepo,
                             ImportRunLogRepository runLogRepo,
                             PlatformTransactionManager txManager) {
        this.client = client;
        this.itemRepo = itemRepo;
        this.enquirerRepo = enquirerRepo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * Full current-term backfill, but only if it has not already completed successfully. Gated on a
     * SUCCESS run-log rather than on row count, so an interrupted backfill (e.g. a mid-run redeploy)
     * resumes on the next boot instead of being locked out by the partial rows it left behind. The
     * scan is idempotent (upsert by external id), so resuming just re-touches what it already had.
     */
    public void backfillCurrentTermIfNeeded() {
        boolean done = runLogRepo
                .findFirstBySourceNameAndJobNameOrderByStartedAtDesc(client.sourceName(), JOB_BACKFILL)
                .map(r -> "SUCCESS".equals(r.getStatus()))
                .orElse(false);
        if (done) {
            log.info("oversight backfill already completed — skipping");
            return;
        }
        run(JOB_BACKFILL, TERM_START, TERM_START);
    }

    /** Daily incremental: new questions (short window) + new answers (wider, to catch late replies). */
    public ImportRunLog refreshRecent() {
        LocalDate today = LocalDate.now();
        return run(JOB_REFRESH, today.minusDays(21), today.minusDays(60));
    }

    private ImportRunLog run(String jobName, LocalDate questionsSince, LocalDate answersSince) {
        ImportRunLog runLog = runLogRepo.save(ImportRunLog.builder()
                .sourceName(client.sourceName()).jobName(jobName)
                .startedAt(Instant.now()).status("RUNNING").build());
        int seen = 0, upserted = 0;
        try {
            upserted += ingestQuestions(T_WRITTEN_Q, OversightItem.Kind.WRITTEN_QUESTION, questionsSince);
            upserted += ingestQuestions(T_INTERP_Q, OversightItem.Kind.INTERPELLATION, questionsSince);
            int a1 = ingestAnswers(T_WRITTEN_A, answersSince);
            int a2 = ingestAnswers(T_INTERP_A, answersSince);
            seen = upserted + a1 + a2;
            runLog.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("oversight import failed", e);
            runLog.setStatus("FAILED");
            runLog.setErrorMessage(e.getMessage());
        } finally {
            runLog.setRecordsSeen(seen);
            runLog.setRecordsUpserted(upserted);
            runLog.setFinishedAt(Instant.now());
            runLogRepo.save(runLog);
        }
        return runLog;
    }

    /** Page the register for a question type (newest first), upserting each until older than the cutoff. */
    private int ingestQuestions(String documentType, OversightItem.Kind kind, LocalDate stopBefore) {
        int upserted = 0;
        for (int page = 0; page < SAFETY_PAGES; page++) {
            JsonNode resp = fetchPageWithRetry(documentType, page);
            List<JsonNode> content = contentOf(resp);
            if (content.isEmpty()) break;
            boolean reachedOld = false;
            for (JsonNode entry : content) {
                LocalDate created = date(text(entry, "created"));
                if (created != null && created.isBefore(stopBefore)) { reachedOld = true; continue; }
                String uuid = text(entry, "uuid");
                if (uuid == null) continue;
                // Resume cheaply: a question we already stored never changes, so skip the detail fetch.
                if (itemRepo.existsByExternalId(uuid)) continue;
                try {
                    if (Boolean.TRUE.equals(tx.execute(status -> upsertQuestion(uuid, kind)))) upserted++;
                } catch (Exception e) {
                    log.warn("oversight question upsert failed for {}: {}", uuid, e.toString());
                }
            }
            if (reachedOld || isLastPage(resp)) break;
        }
        return upserted;
    }

    private Boolean upsertQuestion(String uuid, OversightItem.Kind kind) {
        JsonNode d = client.fetchDocumentDetail(uuid);
        if (d == null) return false;
        List<JsonNode> enquirers = arrayOf(d, "enquirers");
        if (enquirers.isEmpty()) return false; // follow-ups / administrative docs — not attributable
        OversightItem item = itemRepo.findByExternalId(uuid)
                .orElseGet(() -> OversightItem.builder().id(UUID.randomUUID()).externalId(uuid).build());
        item.setKind(kind);
        item.setTitle(orDefault(text(d, "title"), "(pealkiri puudub)"));
        item.setSubmittedOn(date(text(d, "submittingDate")));
        item.setAnswerDeadline(date(text(d, "answerDeadline")));
        item.setAddresseeName(nestedText(d, "addressee", "name"));
        item.setVolumeExternalId(nestedText(d, "volume", "uuid"));
        item.setMembershipNumber(intOrNull(d, "membership"));
        if (item.getImportedAt() == null) item.setImportedAt(Instant.now());
        itemRepo.save(item);

        enquirerRepo.deleteByOversightItemId(item.getId());
        for (JsonNode e : enquirers) {
            String extId = text(e, "uuid");
            if (extId == null) continue;
            enquirerRepo.save(OversightEnquirer.builder()
                    .oversightItemId(item.getId())
                    .memberExternalId(extId)
                    .memberName(text(e, "name"))
                    .build());
        }
        return true;
    }

    /** Page the register for an answer type and attach each answer to its question via the volume. */
    private int ingestAnswers(String documentType, LocalDate stopBefore) {
        int linked = 0;
        for (int page = 0; page < SAFETY_PAGES; page++) {
            JsonNode resp = fetchPageWithRetry(documentType, page);
            List<JsonNode> content = contentOf(resp);
            if (content.isEmpty()) break;
            boolean reachedOld = false;
            for (JsonNode entry : content) {
                LocalDate created = date(text(entry, "created"));
                if (created != null && created.isBefore(stopBefore)) { reachedOld = true; continue; }
                String uuid = text(entry, "uuid");
                if (uuid == null) continue;
                // Resume cheaply: an answer already attached to its question can be skipped.
                if (itemRepo.existsByAnswerExternalId(uuid)) continue;
                try {
                    if (Boolean.TRUE.equals(tx.execute(status -> linkAnswer(uuid)))) linked++;
                } catch (Exception e) {
                    log.warn("oversight answer link failed for {}: {}", uuid, e.toString());
                }
            }
            if (reachedOld || isLastPage(resp)) break;
        }
        return linked;
    }

    /**
     * Fetch one register page, retrying a few times on a transient error (429/circuit-breaker/timeout)
     * so a single blip does not fail the whole run. If it still fails the exception propagates and the
     * run is marked FAILED — but because questions/answers we already have are skipped, the next run
     * resumes cheaply from where this one stopped rather than re-fetching everything.
     */
    private JsonNode fetchPageWithRetry(String documentType, int page) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return client.fetchDocumentsPage(documentType, page, PAGE_SIZE);
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound nf) {
                // The register does not support filtering by this documentType — notably
                // interpellationsAnswerDocument 404s (interpellations are answered orally in the
                // sitting, not via a queryable answer document). Skip the type, don't fail the run.
                log.info("oversight: documentType {} not queryable (404) — skipping", documentType);
                return null;
            } catch (RuntimeException e) {
                last = e;
                log.warn("oversight page fetch {} p{} attempt {}/3 failed: {}",
                        documentType, page, attempt, e.toString());
                sleepQuietly(5000L * attempt);
            }
        }
        throw last;
    }

    private static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private Boolean linkAnswer(String uuid) {
        JsonNode d = client.fetchDocumentDetail(uuid);
        if (d == null) return false;
        String volume = nestedText(d, "volume", "uuid");
        if (volume == null) return false;
        List<OversightItem> questions = itemRepo.findByVolumeExternalId(volume);
        if (questions.isEmpty()) return false; // answer to a question outside our term window
        LocalDate respondedOn = date(text(d, "respondDate"));
        String respondent = nestedText(d, "respondent", "name");
        boolean any = false;
        for (OversightItem q : questions) {
            q.setAnswered(true);
            q.setAnswerExternalId(uuid);
            q.setRespondentName(respondent);
            q.setRespondedOn(respondedOn);
            itemRepo.save(q);
            any = true;
        }
        return any;
    }

    // --- JSON helpers -------------------------------------------------------

    private static List<JsonNode> contentOf(JsonNode resp) {
        if (resp == null) return List.of();
        JsonNode content = resp.path("_embedded").path("content");
        if (!content.isArray()) return List.of();
        List<JsonNode> out = new ArrayList<>();
        content.forEach(out::add);
        return out;
    }

    private static boolean isLastPage(JsonNode resp) {
        JsonNode page = resp == null ? null : resp.get("page");
        if (page == null) return true;
        int number = page.path("number").asInt(0);
        int totalPages = page.path("totalPages").asInt(0);
        return number >= totalPages - 1;
    }

    private static List<JsonNode> arrayOf(JsonNode node, String field) {
        JsonNode arr = node == null ? null : node.get(field);
        if (arr == null || !arr.isArray()) return List.of();
        List<JsonNode> out = new ArrayList<>();
        arr.forEach(out::add);
        return out;
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static String nestedText(JsonNode node, String field, String sub) {
        JsonNode n = node == null ? null : node.get(field);
        return n != null && n.hasNonNull(sub) ? n.get(sub).asText() : null;
    }

    private static Integer intOrNull(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) && node.get(field).isNumber()
                ? node.get(field).asInt() : null;
    }

    private static LocalDate date(String iso) {
        if (iso == null || iso.length() < 10) return null;
        try { return LocalDate.parse(iso.substring(0, 10)); }
        catch (Exception e) { return null; }
    }

    private static String orDefault(String s, String def) {
        return s == null || s.isBlank() ? def : s;
    }
}
