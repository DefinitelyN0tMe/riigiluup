package com.politico.alignment;

import com.politico.person.PlenaryMember;
import com.politico.vote.IndividualVote;
import com.politico.vote.VoteChoice;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PairwiseAgreementService {

    @PersistenceContext
    private final EntityManager em;

    /**
     * Fetch the (left, right) choice pairs for every vote both MPs voted on,
     * then delegate to {@link #count(List)}.
     */
    public Result forPair(PlenaryMember left, PlenaryMember right, Instant from, Instant to) {
        String jpql = """
            select l.choice, r.choice
            from IndividualVote l, IndividualVote r
            where l.voteEvent = r.voteEvent
              and l.plenaryMember = :left
              and r.plenaryMember = :right
              and (:from is null or l.voteEvent.startedAt >= :from)
              and (:to is null or l.voteEvent.startedAt <= :to)
            """;
        List<Tuple> rows = em.createQuery(jpql, Tuple.class)
                .setParameter("left", left)
                .setParameter("right", right)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        List<Pair> pairs = new ArrayList<>(rows.size());
        for (Tuple t : rows) {
            pairs.add(new Pair(t.get(0, VoteChoice.class), t.get(1, VoteChoice.class)));
        }
        return count(pairs);
    }

    /**
     * Recent votes where the two MPs cast comparable but different choices.
     * Newest-first, limited to {@code limit}.
     */
    public List<IndividualVote[]> recentDisagreements(
            PlenaryMember left, PlenaryMember right, int limit
    ) {
        String jpql = """
            select l, r
            from IndividualVote l, IndividualVote r
            where l.voteEvent = r.voteEvent
              and l.plenaryMember = :left
              and r.plenaryMember = :right
              and l.choice in (
                  com.politico.vote.VoteChoice.FOR,
                  com.politico.vote.VoteChoice.AGAINST,
                  com.politico.vote.VoteChoice.ABSTAINED)
              and r.choice in (
                  com.politico.vote.VoteChoice.FOR,
                  com.politico.vote.VoteChoice.AGAINST,
                  com.politico.vote.VoteChoice.ABSTAINED)
              and l.choice <> r.choice
            order by l.voteEvent.startedAt desc
            """;
        List<Tuple> rows = em.createQuery(jpql, Tuple.class)
                .setParameter("left", left)
                .setParameter("right", right)
                .setMaxResults(limit)
                .getResultList();
        List<IndividualVote[]> out = new ArrayList<>(rows.size());
        for (Tuple t : rows) {
            out.add(new IndividualVote[]{
                    t.get(0, IndividualVote.class),
                    t.get(1, IndividualVote.class)});
        }
        return out;
    }

    /** Pure math over a list of (leftChoice, rightChoice) pairs. */
    public static Result count(List<Pair> pairs) {
        int same = 0, diff = 0, oneNonPart = 0;
        for (Pair p : pairs) {
            boolean lc = isComparable(p.left);
            boolean rc = isComparable(p.right);
            if (!lc || !rc) {
                oneNonPart++;
            } else if (p.left == p.right) {
                same++;
            } else {
                diff++;
            }
        }
        int total = same + diff + oneNonPart;
        int comparableDenominator = same + diff;
        Double rate = comparableDenominator == 0 ? null : (double) same / (double) comparableDenominator;
        return new Result(same, diff, oneNonPart, total, rate);
    }

    private static boolean isComparable(VoteChoice c) {
        return c == VoteChoice.FOR || c == VoteChoice.AGAINST || c == VoteChoice.ABSTAINED;
    }

    public record Pair(VoteChoice left, VoteChoice right) {}

    public record Result(
            int sameCount,
            int diffCount,
            int oneNotParticipatingCount,
            int totalOverlap,
            Double agreementRate
    ) {}
}
