package com.riigiluup.api;

import java.time.Instant;

public record DataStatusDto(
        String sourceName,
        String jobName,
        Instant lastRunAt,
        String lastRunStatus,
        int lastRunRecords
) {}
