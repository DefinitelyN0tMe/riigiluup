package com.riigiluup.admin;

import java.time.Instant;
import java.util.List;

public record AdminStatusDto(
        List<JobStatus> jobs,
        List<SnapshotSummary> snapshotsByEntity,
        DomainCounts counts,
        Instant generatedAt
) {
    public record JobStatus(
            String sourceName,
            String jobName,
            String lastRunStatus,
            Instant lastRunAt,
            int recordsSeen,
            int recordsUpserted,
            String errorMessage
    ) {}

    public record SnapshotSummary(String entityType, long count) {}

    public record DomainCounts(
            long plenaryMembers,
            long groups,
            long voteEvents,
            long individualVotes,
            long legislativeItems
    ) {}
}
