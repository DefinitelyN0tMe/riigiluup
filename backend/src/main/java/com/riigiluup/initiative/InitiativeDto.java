package com.riigiluup.initiative;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** All initiative-facing records in one file, mirroring the AnalyticsDto convention. */
public final class InitiativeDto {

    private InitiativeDto() {
    }

    /** One committee assignment; groupId is null when the slug has no active committee. */
    public record CommitteeRef(String slug, String name, UUID groupId) {
    }

    public record LinkedBill(UUID id, String title, String linkedBy, Instant linkedAt) {
    }

    public record ListItem(
            Long id,
            String externalId,
            String title,
            String authors,
            String phase,
            Integer signatureCount,
            String decision,
            Instant sentToParliamentAt,
            List<CommitteeRef> committees,
            String sourceUrl
    ) {
    }

    public record ListPage(List<ListItem> items, int page, int totalPages, long totalElements) {
    }

    public record Detail(
            Long id,
            String externalId,
            String title,
            String authors,
            String phase,
            Integer signatureCount,
            Integer threshold,
            Instant publishedAt,
            Instant signingStartedAt,
            Instant signingEndsAt,
            Instant lastSignedAt,
            Instant sentToParliamentAt,
            String decision,
            Instant finishedInParliamentAt,
            Instant sentToGovernmentAt,
            Instant finishedInGovernmentAt,
            List<CommitteeRef> committees,
            LinkedBill linkedBill,
            String sourceUrl
    ) {
    }

    /** shareOfTargeted is null only when the dataset is empty. */
    public record FunnelStep(String key, long count, Double shareOfTargeted) {
    }

    public record DecisionCount(String decision, long count) {
    }

    public record CommitteeCount(String slug, String name, UUID groupId, long count) {
    }

    public record Funnel(
            List<FunnelStep> steps,
            List<DecisionCount> decisions,
            List<CommitteeCount> committees,
            Double medianDaysToDecision,
            long medianSampleSize,
            long reachedThresholdButNeverSent,
            long sentBelowThreshold
    ) {
    }
}
