package com.riigiluup.question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GovernmentQuestionRepository extends JpaRepository<GovernmentQuestion, Long> {

    Optional<GovernmentQuestion> findBySourceNameAndExternalId(String sourceName, String externalId);

    /**
     * Response-latency aggregate per addressee (minister). Returned volumes
     * (answer_deadline IS NULL) are excluded everywhere; "overdue now" counts open
     * questions whose deadline already passed.
     */
    @Query(nativeQuery = true, value = """
            SELECT q.addressee_name AS addresseeName,
                   max(q.addressee_role) AS addresseeRole,
                   count(*) AS total,
                   count(q.answered_date) AS answered,
                   count(*) FILTER (WHERE q.answered_date IS NOT NULL
                                      AND q.answered_date <= q.answer_deadline) AS answeredOnTime,
                   percentile_cont(0.5) WITHIN GROUP (
                       ORDER BY (q.answered_date - q.submitting_date))
                       FILTER (WHERE q.answered_date IS NOT NULL) AS medianDaysToAnswer,
                   count(*) FILTER (WHERE q.answered_date IS NULL
                                      AND q.answer_deadline < CURRENT_DATE) AS overdueNow
            FROM government_question q
            WHERE q.answer_deadline IS NOT NULL
              AND q.addressee_name IS NOT NULL
              AND q.submitting_date >= cast(:since AS date)
            GROUP BY q.addressee_name
            HAVING count(*) >= cast(:minQuestions AS integer)
            ORDER BY overdueNow DESC, medianDaysToAnswer DESC NULLS LAST
            """)
    List<LatencyRow> latencyByAddressee(@Param("since") LocalDate since,
                                        @Param("minQuestions") int minQuestions);

    interface LatencyRow {
        String getAddresseeName();
        String getAddresseeRole();
        long getTotal();
        long getAnswered();
        long getAnsweredOnTime();
        Double getMedianDaysToAnswer();
        long getOverdueNow();
    }
}
