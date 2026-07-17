package com.riigiluup.initiative;

import com.riigiluup.legislation.LegislativeItemRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Initiative queries. The funnel is computed on the fly with native SQL: the corpus is ~1141
 * rows, so a precomputed table would be the same over-engineering as the rejected
 * mv_politician_summary for 101 MPs.
 *
 * <p>Every metric here is restricted to destination='parliament'. Municipal initiatives have
 * a 1%-of-residents threshold instead of the flat 1000 and would corrupt each denominator.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InitiativeService {

    /**
     * Signature threshold for a collective address (kollektiivne pöördumine) to the Riigikogu:
     * at least 1000 supporting signatures ("vähemalt 1000 toetusallkirja"), set by the
     * Märgukirjale ja selgitustaotlusele vastamise ning kollektiivse pöördumise esitamise
     * seadus (MSVS) § 7¹ lõige 1 (added by RT I, 01.04.2014, 1 — jõust. 11.04.2014; unchanged
     * by the most recent amendment, RT I, 18.12.2024, 2, which only touched §6's response
     * deadline). Verified against the current consolidated text at
     * https://www.riigiteataja.ee/akt/MSVS#para7b1 (2026-07-17).
     *
     * <p>RKKTS §152⁹–152¹³ (added by the same 2014 act) governs only the Riigikogu's internal
     * procedure for deciding whether to accept an already-submitted pöördumine for processing
     * — it does not itself set the signature threshold.
     */
    public static final int PARLIAMENT_THRESHOLD = 1000;

    private static final String PARLIAMENT = "parliament";

    private final EntityManager em;
    private final InitiativeRepository repo;
    private final InitiativeCommitteeLinkRepository linkRepo;
    private final LegislativeItemRepository legislativeItemRepo;

    /** Source page for one initiative — every fact on our pages links back to it. */
    public static String sourceUrl(String externalId) {
        return "https://rahvaalgatus.ee/initiatives/" + externalId;
    }

    public InitiativeDto.ListPage list(
            String q, String phase, String decision, String committee, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE i.destination = :dest ");
        Map<String, Object> params = new HashMap<>();
        params.put("dest", PARLIAMENT);
        if (q != null && !q.isBlank()) {
            where.append(" AND i.title ILIKE :q ");
            params.put("q", "%" + q.trim() + "%");
        }
        if (phase != null && !phase.isBlank()) {
            where.append(" AND i.phase = :phase ");
            params.put("phase", InitiativePhase.fromSlug(phase).slug());
        }
        if (decision != null && !decision.isBlank()) {
            where.append(" AND i.parliament_decision = :decision ");
            params.put("decision", ParliamentDecision.fromSlug(decision).slug());
        }
        if (committee != null && !committee.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM initiative_committee ic "
                    + "WHERE ic.initiative_id = i.id AND ic.committee_slug = :committee) ");
            params.put("committee", committee);
        }

        var countQuery = em.createNativeQuery("SELECT count(*) FROM initiative i" + where);
        params.forEach(countQuery::setParameter);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        var idQuery = em.createNativeQuery(
                "SELECT i.id FROM initiative i" + where
                        + " ORDER BY i.sent_to_parliament_at DESC NULLS LAST, i.published_at DESC NULLS LAST"
                        + " LIMIT :size OFFSET :offset");
        params.forEach(idQuery::setParameter);
        idQuery.setParameter("size", size);
        idQuery.setParameter("offset", (long) page * size);
        List<Long> ids = ((List<?>) idQuery.getResultList()).stream()
                .map(o -> ((Number) o).longValue()).toList();

        List<InitiativeDto.ListItem> items = ids.stream()
                .map(id -> repo.findById(id).orElseThrow())
                .map(this::toListItem)
                .toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new InitiativeDto.ListPage(items, page, totalPages, total);
    }

    public Optional<InitiativeDto.Detail> detail(Long id) {
        return repo.findById(id)
                .filter(i -> PARLIAMENT.equals(i.getDestination()))
                .map(i -> new InitiativeDto.Detail(
                        i.getId(),
                        i.getExternalId(),
                        i.getTitle(),
                        i.getAuthors(),
                        i.getPhase() == null ? null : i.getPhase().slug(),
                        i.getSignatureCount(),
                        PARLIAMENT.equals(i.getDestination()) ? PARLIAMENT_THRESHOLD : null,
                        i.getPublishedAt(),
                        i.getSigningStartedAt(),
                        i.getSigningEndsAt(),
                        i.getLastSignedAt(),
                        i.getSentToParliamentAt(),
                        i.getParliamentDecision() == null ? null : i.getParliamentDecision().slug(),
                        i.getFinishedInParliamentAt(),
                        i.getSentToGovernmentAt(),
                        i.getFinishedInGovernmentAt(),
                        committeesOf(i.getId()),
                        linkedBill(i),
                        sourceUrl(i.getExternalId())));
    }

    /** Reverse link for the bill page: "this act started as a citizen initiative". */
    public List<InitiativeDto.ListItem> byLegislativeItem(UUID legislativeItemId) {
        return repo.findByLegislativeItemId(legislativeItemId).stream()
                .map(this::toListItem)
                .toList();
    }

    private InitiativeDto.ListItem toListItem(Initiative i) {
        return new InitiativeDto.ListItem(
                i.getId(),
                i.getExternalId(),
                i.getTitle(),
                i.getAuthors(),
                i.getPhase() == null ? null : i.getPhase().slug(),
                i.getSignatureCount(),
                i.getParliamentDecision() == null ? null : i.getParliamentDecision().slug(),
                i.getSentToParliamentAt(),
                committeesOf(i.getId()),
                sourceUrl(i.getExternalId()));
    }

    private InitiativeDto.LinkedBill linkedBill(Initiative i) {
        if (i.getLegislativeItemId() == null) return null;
        return legislativeItemRepo.findById(i.getLegislativeItemId())
                .map(b -> new InitiativeDto.LinkedBill(
                        b.getId(), b.getTitle(), i.getLinkedBy(), i.getLinkedAt()))
                .orElse(null);
    }

    private List<InitiativeDto.CommitteeRef> committeesOf(Long initiativeId) {
        return linkRepo.findByInitiativeId(initiativeId).stream()
                .map(l -> new InitiativeDto.CommitteeRef(
                        l.getCommitteeSlug(),
                        InitiativeCommittee.fromSlug(l.getCommitteeSlug())
                                .map(InitiativeCommittee::committeeName).orElse(null),
                        l.getGroupId()))
                .toList();
    }

    /** Pure math, extracted so the funnel's contract is testable without a database. */
    static List<InitiativeDto.FunnelStep> buildFunnel(
            long targeted, long signing, long threshold, long sent, long decided, long draftAct) {
        List<InitiativeDto.FunnelStep> steps = new ArrayList<>(6);
        steps.add(step("targeted", targeted, targeted));
        steps.add(step("signing", signing, targeted));
        steps.add(step("threshold", threshold, targeted));
        steps.add(step("sent", sent, targeted));
        steps.add(step("decided", decided, targeted));
        steps.add(step("draftAct", draftAct, targeted));
        return steps;
    }

    private static InitiativeDto.FunnelStep step(String key, long count, long baseline) {
        Double share = baseline == 0 ? null : Math.round(count * 1000.0 / baseline) / 10.0;
        return new InitiativeDto.FunnelStep(key, count, share);
    }

    public InitiativeDto.Funnel funnel() {
        Object[] c = (Object[]) em.createNativeQuery("""
                SELECT
                  count(*),
                  count(*) FILTER (WHERE signing_started_at IS NOT NULL),
                  count(*) FILTER (WHERE signature_count >= :threshold),
                  count(*) FILTER (WHERE sent_to_parliament_at IS NOT NULL),
                  count(*) FILTER (WHERE parliament_decision IS NOT NULL),
                  count(*) FILTER (WHERE parliament_decision = 'draft-act-or-national-matter'),
                  count(*) FILTER (WHERE signature_count >= :threshold
                                     AND sent_to_parliament_at IS NULL),
                  count(*) FILTER (WHERE sent_to_parliament_at IS NOT NULL
                                     AND (signature_count IS NULL
                                          OR signature_count < :threshold))
                FROM initiative WHERE destination = :dest
                """)
                .setParameter("threshold", PARLIAMENT_THRESHOLD)
                .setParameter("dest", PARLIAMENT)
                .getSingleResult();

        // Records with a decision but no sent date (14 live) drop out of the median but stay
        // in the step counts — timing gaps must not silently shrink the funnel.
        Object median = em.createNativeQuery("""
                SELECT percentile_cont(0.5) WITHIN GROUP (
                         ORDER BY EXTRACT(EPOCH FROM (finished_in_parliament_at
                                                      - sent_to_parliament_at)) / 86400.0)
                FROM initiative
                WHERE destination = :dest
                  AND sent_to_parliament_at IS NOT NULL
                  AND finished_in_parliament_at IS NOT NULL
                """).setParameter("dest", PARLIAMENT).getSingleResult();

        Number medianSample = (Number) em.createNativeQuery("""
                SELECT count(*) FROM initiative
                WHERE destination = :dest
                  AND sent_to_parliament_at IS NOT NULL
                  AND finished_in_parliament_at IS NOT NULL
                """).setParameter("dest", PARLIAMENT).getSingleResult();

        List<Object[]> decisionRows = em.createNativeQuery("""
                SELECT parliament_decision, count(*) FROM initiative
                WHERE destination = :dest AND parliament_decision IS NOT NULL
                GROUP BY parliament_decision ORDER BY count(*) DESC
                """).setParameter("dest", PARLIAMENT).getResultList();

        List<Object[]> committeeRows = em.createNativeQuery("""
                SELECT ic.committee_slug, ic.group_id, count(*)
                FROM initiative_committee ic
                JOIN initiative i ON i.id = ic.initiative_id
                WHERE i.destination = :dest
                GROUP BY ic.committee_slug, ic.group_id ORDER BY count(*) DESC
                """).setParameter("dest", PARLIAMENT).getResultList();

        List<InitiativeDto.DecisionCount> decisions = decisionRows.stream()
                .map(r -> new InitiativeDto.DecisionCount(
                        (String) r[0], ((Number) r[1]).longValue()))
                .toList();

        List<InitiativeDto.CommitteeCount> committees = committeeRows.stream()
                .map(r -> new InitiativeDto.CommitteeCount(
                        (String) r[0],
                        InitiativeCommittee.fromSlug((String) r[0])
                                .map(InitiativeCommittee::committeeName).orElse(null),
                        (UUID) r[1],
                        ((Number) r[2]).longValue()))
                .toList();

        return new InitiativeDto.Funnel(
                buildFunnel(num(c[0]), num(c[1]), num(c[2]), num(c[3]), num(c[4]), num(c[5])),
                decisions,
                committees,
                median == null ? null : Math.round(((Number) median).doubleValue() * 10) / 10.0,
                medianSample.longValue(),
                num(c[6]),
                num(c[7]));
    }

    private static long num(Object o) {
        return ((Number) o).longValue();
    }
}
