package com.riigiluup.initiative;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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
     * Signature threshold for a collective address to Riigikogu.
     *
     * <p>TODO(before merge): verify the exact statute (MSVS §4¹ vs RKKTS §152¹) against
     * riigiteataja.ee and cite it in the methodology page. Do NOT cite from memory — the spec
     * flagged this deliberately.
     */
    public static final int PARLIAMENT_THRESHOLD = 1000;

    private static final String PARLIAMENT = "parliament";

    private final EntityManager em;

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
