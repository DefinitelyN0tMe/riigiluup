package com.riigiluup.statistics;

public record VotingStats(
        int totalVotings,
        int participated,
        Double participationRate,
        String methodologyNote,
        String sourceUrl
) {}
