package com.riigiluup.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final ErjkClient client;
    private final PartyReceiptRepository repo;

    public PartyFinanceImporter(ErjkClient client, PartyReceiptRepository repo) {
        this.client = client;
        this.repo = repo;
    }

    @Scheduled(cron = "0 45 5 5 * *", zone = "Europe/Tallinn") // 5th of each month, 05:45
    public void scheduledImport() {
        try {
            importAll();
        } catch (Exception e) {
            log.warn("Scheduled ERJK party-finance import failed", e);
        }
    }

    @Transactional
    public int importAll() {
        List<ErjkReceiptDto> receipts = client.fetchAllReceipts();
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
        log.info("Imported {} ERJK party-receipt rows", n);
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
