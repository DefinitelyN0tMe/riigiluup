package com.riigiluup.statistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.riigiluup.common.CacheConfig;
import com.riigiluup.ingestion.riigikogu.RiigikoguClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    /** Start of the current (XV) Riigikogu term — default period for aggregate metrics. */
    public static final LocalDate TERM_START = LocalDate.of(2023, 4, 10);

    private final RiigikoguClient client;
    private final ParticipationCacheStore cacheStore;

    @Cacheable(
            value = CacheConfig.CACHE_PARTICIPATION,
            key = "#memberUuid + ':' + #from + ':' + #to",
            unless = "#result.participationRate == null")
    public ParticipationStats participation(String memberUuid, LocalDate from, LocalDate to) {
        String url = "https://api.riigikogu.ee/api/statistics/participations/member/"
                + memberUuid + "?startDate=" + from + "&endDate=" + to;
        ParticipationStats fresh = null;
        try {
            JsonNode json = client.fetchParticipationStats(memberUuid, from, to);
            int total = asInt(json, "sittings");
            int attended = asInt(json, "participated");
            Double rate = ratio(attended, total);
            fresh = new ParticipationStats(
                    total, attended, rate,
                    "Attendance-check presence reported by Riigikogu API (" + from + " → " + to + ")",
                    url);
        } catch (Exception e) {
            log.warn("participation stats fetch failed for {} [{} -> {}]",
                    memberUuid, from, to, e);
        }
        // A good live figure is persisted as the new last-good and returned.
        if (fresh != null && fresh.participationRate() != null) {
            cacheStore.save(memberUuid, fresh.totalSittings(), fresh.attended(),
                    fresh.participationRate(), from, to);
            return fresh;
        }
        // Live fetch failed or the source returned an empty 0/0 — serve the last value we
        // persisted so an active MP never flips to "0 of 0" during a source outage. Only fall
        // through to the "unavailable" sentinel if we have never captured a good figure yet.
        return cacheStore.lastGood(memberUuid, url)
                .orElse(fresh != null ? fresh
                        : new ParticipationStats(0, 0, null,
                                "Data currently unavailable from Riigikogu API", url));
    }

    @Cacheable(
            value = CacheConfig.CACHE_VOTING,
            key = "#memberUuid + ':' + #from + ':' + #to",
            unless = "#result.participationRate == null")
    public VotingStats voting(String memberUuid, LocalDate from, LocalDate to) {
        String url = "https://api.riigikogu.ee/api/statistics/votings/member/"
                + memberUuid + "?startDate=" + from + "&endDate=" + to;
        try {
            JsonNode json = client.fetchVotingStats(memberUuid, from, to);
            int inFavor = asInt(json, "inFavor");
            int against = asInt(json, "against");
            int abstained = asInt(json, "abstained");
            int neutral = asInt(json, "neutral");
            int present = asInt(json, "present");
            int absent = asInt(json, "absent");
            int participated = inFavor + against + abstained;
            int total = participated + neutral + present + absent;
            Double rate = ratio(participated, total);
            return new VotingStats(
                    total, participated, rate,
                    "Voting participation = (FOR + AGAINST + ABSTAINED) / eligible roll-call votes ("
                            + from + " → " + to + ")",
                    url);
        } catch (Exception e) {
            log.warn("voting stats fetch failed for {} [{} -> {}]",
                    memberUuid, from, to, e);
            return new VotingStats(0, 0, null,
                    "Data currently unavailable from Riigikogu API", url);
        }
    }

    private static int asInt(JsonNode json, String key) {
        if (json == null || !json.hasNonNull(key) || !json.get(key).canConvertToInt()) return 0;
        return json.get(key).asInt();
    }

    private static Double ratio(int num, int den) {
        return den == 0 ? null : (double) num / (double) den;
    }
}
