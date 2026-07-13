package com.politico.alignment;

import com.politico.person.PlenaryMember;
import com.politico.vote.IndividualVote;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupAlignmentService {

    @PersistenceContext
    private final EntityManager em;

    /**
     * Per-MP group-alignment rate:
     *   eligible = MP votes where choice is comparable AND the MP's faction had a clear majority
     *   matches  = eligible votes where MP.choice = faction.majorityChoice
     */
    public Result forMember(PlenaryMember member, Instant from, Instant to) {
        String jpql = """
            select
                sum(case when iv.choice = a.majorityChoice then 1 else 0 end),
                count(iv)
            from IndividualVote iv
            join VoteFactionAlignment a
              on a.voteEvent = iv.voteEvent
             and a.factionExternalId = iv.factionExternalId
            where iv.plenaryMember = :member
              and a.hasClearMajority = true
              and iv.choice in (
                  com.politico.vote.VoteChoice.FOR,
                  com.politico.vote.VoteChoice.AGAINST,
                  com.politico.vote.VoteChoice.ABSTAINED)
              and (cast(:from as instant) is null or iv.voteEvent.startedAt >= :from)
              and (cast(:to as instant) is null or iv.voteEvent.startedAt <= :to)
            """;
        Tuple row = em.createQuery(jpql, Tuple.class)
                .setParameter("member", member)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        long matches = row.get(0, Long.class) == null ? 0L : row.get(0, Long.class);
        long eligible = row.get(1, Long.class) == null ? 0L : row.get(1, Long.class);
        Double rate = eligible == 0 ? null : (double) matches / (double) eligible;
        return new Result(rate, (int) matches, (int) eligible);
    }

    /**
     * Recent votes where the MP diverged from their faction's clear majority.
     * Returned newest-first, limited to {@code limit}.
     */
    public List<IndividualVote> recentDeviations(PlenaryMember member, int limit) {
        String jpql = """
            select iv from IndividualVote iv
            join VoteFactionAlignment a
              on a.voteEvent = iv.voteEvent
             and a.factionExternalId = iv.factionExternalId
            where iv.plenaryMember = :member
              and a.hasClearMajority = true
              and iv.choice in (
                  com.politico.vote.VoteChoice.FOR,
                  com.politico.vote.VoteChoice.AGAINST,
                  com.politico.vote.VoteChoice.ABSTAINED)
              and iv.choice <> a.majorityChoice
            order by iv.voteEvent.startedAt desc
            """;
        return em.createQuery(jpql, IndividualVote.class)
                .setParameter("member", member)
                .setMaxResults(limit)
                .getResultList();
    }

    public record Result(Double rate, int matches, int eligible) {}
}
