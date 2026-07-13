package com.politico.statistics;

public record ParticipationStats(
        int totalSittings,
        int attended,
        Double participationRate,
        String methodologyNote,
        String sourceUrl
) {}
