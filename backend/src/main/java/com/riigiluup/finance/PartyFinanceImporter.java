package com.riigiluup.finance;

import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Full-replace import of ERJK party income data. The dataset is small (all parties ×
 * income types × years), so we refresh the whole table. ERJK reports quarterly, so a
 * monthly schedule is plenty; also admin-triggerable.
 */
@Service
public class PartyFinanceImporter {

    private static final Logger log = LoggerFactory.getLogger(PartyFinanceImporter.class);
    private static final String SOURCE_NAME = "erjk";
    private static final String JOB_NAME = "party-finance.full-refresh";

    private final ErjkClient client;
    private final PartyReceiptRepository repo;
    private final ImportRunLogRepository runLogRepo;
    private final TransactionTemplate tx;

    public PartyFinanceImporter(ErjkClient client, PartyReceiptRepository repo,
                                ImportRunLogRepository runLogRepo,
                                PlatformTransactionManager txManager) {
        this.client = client;
        this.repo = repo;
        this.runLogRepo = runLogRepo;
        this.tx = new TransactionTemplate(txManager);
    }

    @Scheduled(cron = "0 45 5 5 * *", zone = "Europe/Tallinn") // 5th of each month, 05:45
    public void scheduledImport() {
        try {
            importAll();
        } catch (Exception e) {
            log.warn("Scheduled ERJK party-finance import failed", e);
        }
    }

    public int importAll() {
        ImportRunLog run = runLogRepo.save(ImportRunLog.builder()
                .sourceName(SOURCE_NAME)
                .jobName(JOB_NAME)
                .startedAt(Instant.now())
                .status("RUNNING")
                .build());
        int seen = 0;
        int n = 0;
        try {
            List<ErjkReceiptDto> receipts = client.fetchAllReceipts(); // network, outside any tx
            seen = receipts.size();
            // An empty payload is indistinguishable from a silently broken source — a
            // full-replace here would wipe the table, so keep the existing rows instead.
            if (receipts.isEmpty()) {
                log.warn("ERJK returned zero receipts — keeping existing party-receipt rows");
                run.setStatus("FAILED");
                run.setErrorMessage("source returned zero receipts; existing rows kept");
                return 0;
            }
            n = tx.execute(status -> replaceAll(receipts));
            run.setStatus("SUCCESS");
            log.info("Imported {} ERJK party-receipt rows", n);
        } catch (Exception e) {
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            run.setRecordsSeen(seen);
            run.setRecordsUpserted(n);
            run.setFinishedAt(Instant.now());
            runLogRepo.save(run);
        }
        return n;
    }

    private int replaceAll(List<ErjkReceiptDto> receipts) {
        repo.deleteAllReceipts(); // bulk delete executes before the inserts flush
        Instant now = Instant.now();
        int n = 0;
        for (ErjkReceiptDto r : receipts) {
            Integer partyId = parseInt(r.partyId());
            Integer year = parseInt(r.period());
            BigDecimal amount = parseAmount(r.amount());
            if (partyId == null || year == null || amount == null
                    || r.categoryId() == null || r.categoryName() == null) {
                continue;
            }
            repo.save(PartyReceipt.builder()
                    .erjkPartyId(partyId)
                    .partyName(r.partyName())
                    .categoryId(r.categoryId())
                    .categoryName(r.categoryName())
                    .periodYear(year)
                    .amount(amount)
                    .importedAt(now)
                    .build());
            n++;
        }
        return n;
    }

    private static Integer parseInt(String s) {
        try { return s == null || s.isBlank() ? null : Integer.valueOf(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private static BigDecimal parseAmount(String s) {
        try { return s == null || s.isBlank() ? null : new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
