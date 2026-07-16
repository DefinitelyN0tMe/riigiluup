package com.politico.ingestion.riigikogu;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-shot admin utility: re-link legislative_sponsorship rows to plenary_member
 * for cases where the sponsor was unknown at bill-ingest time but later got
 * materialised as a historical MP stub (via the vote-import stub-fix).
 *
 * <p>Before the fix, if LegislativeItemImporter saw an initiator UUID that wasn't
 * in plenary_member, it stored the sponsorship with plenary_member_id = NULL.
 * Now that vote-import materialises those UUIDs on the fly, we can back-fill
 * the NULL FKs.
 *
 * <p>Runs as a single UPDATE with a subquery — no per-row loop.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SponsorRelinker {

    @PersistenceContext
    private final EntityManager em;

    @Transactional
    public int relinkOrphanSponsors() {
        String sql = """
            UPDATE legislative_sponsorship s
            SET plenary_member_id = m.id
            FROM plenary_member m
            WHERE s.sponsor_kind = 'PLENARY_MEMBER'
              AND s.plenary_member_id IS NULL
              AND s.external_id IS NOT NULL
              AND m.external_id = s.external_id
              AND m.source_name = 'riigikogu'
            """;
        int updated = em.createNativeQuery(sql).executeUpdate();
        log.info("relinked {} orphan legislative_sponsorship rows to plenary_member", updated);
        return updated;
    }
}
