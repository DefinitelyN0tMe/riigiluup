package com.politico.statistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.politico.common.CacheConfig;
import com.politico.ingestion.riigikogu.RiigikoguClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final RiigikoguClient client;

    @Cacheable(value = CacheConfig.CACHE_PARTICIPATION, key = "#memberUuid")
    public ParticipationStats participation(String memberUuid) {
        try {
            JsonNode json = client.fetchParticipationStats(memberUuid);
            int total = asInt(json, "totalSittings", "total", "totalCount");
            int attended = asInt(json, "attended", "present", "attendedCount");
            Double rate = ratio(attended, total);
            return new ParticipationStats(
                    total, attended, rate,
                    "Attendance rate reported by Riigikogu API",
                    "https://api.riigikogu.ee/api/statistics/participations/member/" + memberUuid
            );
        } catch (Exception e) {
            log.warn("participation stats fetch failed for {}: {}", memberUuid, e.toString());
            return new ParticipationStats(0, 0, null,
                    "Data currently unavailable from Riigikogu API",
                    "https://api.riigikogu.ee/api/statistics/participations/member/" + memberUuid);
        }
    }

    @Cacheable(value = CacheConfig.CACHE_VOTING, key = "#memberUuid")
    public VotingStats voting(String memberUuid) {
        try {
            JsonNode json = client.fetchVotingStats(memberUuid);
            int total = asInt(json, "totalVotings", "total", "totalCount");
            int participated = asInt(json, "participated", "count", "participationCount");
            Double rate = ratio(participated, total);
            return new VotingStats(
                    total, participated, rate,
                    "Voting participation rate reported by Riigikogu API",
                    "https://api.riigikogu.ee/api/statistics/votings/member/" + memberUuid
            );
        } catch (Exception e) {
            log.warn("voting stats fetch failed for {}: {}", memberUuid, e.toString());
            return new VotingStats(0, 0, null,
                    "Data currently unavailable from Riigikogu API",
                    "https://api.riigikogu.ee/api/statistics/votings/member/" + memberUuid);
        }
    }

    private static int asInt(JsonNode json, String... candidateKeys) {
        if (json == null) return 0;
        for (String k : candidateKeys) {
            if (json.hasNonNull(k) && json.get(k).canConvertToInt()) return json.get(k).asInt();
        }
        return 0;
    }

    private static Double ratio(int num, int den) {
        return den == 0 ? null : (double) num / (double) den;
    }
}
