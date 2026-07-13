package com.politico.statistics;

public record VotingStats(
        int totalVotings,
        int participated,
        Double participationRate,
        String methodologyNote,
        String sourceUrl
) {}
