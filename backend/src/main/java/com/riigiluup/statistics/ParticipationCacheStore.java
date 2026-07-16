package com.riigiluup.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Reads/writes the persisted last-good attendance figure. Separate bean (not StatisticsService)
 * so the write can run in its own {@code REQUIRES_NEW} transaction — the profile request that
 * triggers it holds a read-only transaction, which would otherwise silently drop the write.
 */
@Service
@RequiredArgsConstructor
public class ParticipationCacheStore {

    private final MemberParticipationCacheRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String memberExternalId, int sittings, int attended, double rate,
                     LocalDate from, LocalDate to) {
        MemberParticipationCache row = repo.findById(memberExternalId)
                .orElseGet(() -> {
                    MemberParticipationCache c = new MemberParticipationCache();
                    c.setMemberExternalId(memberExternalId);
                    return c;
                });
        row.setSittings(sittings);
        row.setAttended(attended);
        row.setRate(rate);
        row.setPeriodFrom(from);
        row.setPeriodTo(to);
        row.setFetchedAt(Instant.now());
        repo.save(row);
    }

    @Transactional(readOnly = true)
    public Optional<ParticipationStats> lastGood(String memberExternalId, String url) {
        return repo.findById(memberExternalId).map(c -> new ParticipationStats(
                c.getSittings(), c.getAttended(), c.getRate(),
                "Last figure reported by Riigikogu (as of " + c.getFetchedAt()
                        + "); the live source is temporarily unavailable",
                url));
    }
}
