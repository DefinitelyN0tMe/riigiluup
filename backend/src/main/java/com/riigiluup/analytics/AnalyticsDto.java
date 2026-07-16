package com.riigiluup.analytics;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** All response records for /api/v1/analytics/**. Kept in one file for compactness. */
public final class AnalyticsDto {
    private AnalyticsDto() {}

    /* ================ FACTION AGREEMENT MATRIX ================ */
    public record FactionAgreementMatrix(
            List<FactionCell> factions,
            List<List<Double>> matrix,     // NxN, values 0..1 or null if no data
            List<List<Integer>> support,   // NxN, number of votes both had comparable clear majority
            int totalVotesConsidered,
            Instant computedAt
    ) {}
    public record FactionCell(String externalId, String name, String shortName, String colorHex, int seats) {}

    /* ================ DISCIPLINE BREAKERS ================ */
    public record DisciplineBreakers(List<DisciplineBreaker> items, Instant computedAt) {}
    public record DisciplineBreaker(
            String memberSlug,
            String memberName,
            String factionExternalId,
            String factionName,
            String factionShortName,
            String factionColorHex,
            int deviations,
            int eligible,
            double deviationRate,       // 0..1
            String exampleVoteDescription,
            String exampleVoteDate,     // ISO date
            UUID exampleVoteId
    ) {}

    /* ================ BILL FLOW (Sankey) ================ */
    public record BillFlow(
            List<BillFlowNode> nodes,
            List<BillFlowLink> links,
            int totalBills,
            Instant computedAt
    ) {}
    public record BillFlowNode(String id, String label, int count) {}
    public record BillFlowLink(String source, String target, int count) {}

    /* ================ ATTENDANCE MATRIX ================ */
    public record AttendanceMatrix(
            List<AttendanceRow> members,
            List<AttendanceCol> sittings,
            /** cell values: "P" present, "A" absent, "-" no data */
            List<String> cells,   // flattened row-major (members * sittings)
            int membersCount,
            int sittingsCount
    ) {}
    public record AttendanceRow(String slug, String shortName, String factionShortName, String factionColorHex,
                                int presentCount, int totalChecks) {}
    public record AttendanceCol(String sittingExternalId, String date, String label) {}

    /* ================ VOTE TIMING HEATMAP ================ */
    /** 7 rows (Mon..Sun) × 24 cols (0..23). Cell = count of vote_events started in that hour bucket. */
    public record VoteTimingHeatmap(List<List<Integer>> cells, int totalVotes, int maxCell) {}

    /* ================ TOPIC TREEMAP ================ */
    public record TopicTreemap(List<TopicSlice> items, int totalBills) {}
    public record TopicSlice(int edid, String label, int billCount, int adoptedCount) {}

    /* ================ BILL VELOCITY ================ */
    public record BillVelocity(
            List<VelocityBucket> buckets,
            int totalAdopted,
            int medianDays,
            int p90Days,
            int fastestDays,
            int slowestDays
    ) {}
    public record VelocityBucket(String label, int daysMax, int count) {}

    /* ================ MP SIMILARITY / SCATTER ================ */
    public record MpSimilarity(List<MpPoint> points, Instant computedAt) {}
    public record MpPoint(
            String slug,
            String name,
            String factionShortName,
            String factionColorHex,
            /** X: coalition (+1) ↔ opposition (-1) — signed agreement with dominant coalition vs main opposition */
            double x,
            /** Y: party loyalty (+1) ↔ dissenter (-1) — group-alignment rate mapped to [-1, 1] */
            double y,
            int totalComparableVotes,
            int deviations
    ) {}

    /** Top-N similar peers for one MP */
    public record MpNeighbors(String slug, List<MpNeighbor> neighbors) {}
    public record MpNeighbor(String slug, String name, String factionShortName, double agreementRate, int overlap) {}

    /* ================ CO-SPONSORSHIP NETWORK ================ */
    public record CoSponsorship(
            List<CoSponsorNode> nodes,
            List<CoSponsorEdge> edges,
            int totalBillsConsidered
    ) {}
    public record CoSponsorNode(String slug, String name, String factionShortName, String factionColorHex, int billsSponsored) {}
    public record CoSponsorEdge(String source, String target, int weight) {}

    /* ================ HIGHLIGHTS BUNDLE ================ */
    public record HighlightsBundle(
            List<AttendanceStreak> streaks,
            List<VoteMargin> tightVotes
    ) {}
    public record AttendanceStreak(String memberSlug, String memberName, String factionShortName,
                                   int consecutivePresent, int totalRecent) {}
    public record VoteMargin(UUID voteId, Integer voteNumber, String description, String startedAt,
                             int forCount, int againstCount, int margin) {}

    /* ================ MEMBER ACTIVITY (most active MPs) ================ */
    public record MemberActivityBoard(List<MemberActivityItem> items, Instant computedAt) {}
    public record MemberActivityItem(
            String memberSlug,
            String memberName,
            String factionShortName,
            String factionColorHex,
            int speeches,
            int questions,
            int interpellations,
            int writtenQuestions
    ) {}

    /* ================ ELECTIONS (personal votes, mandate types) ================ */
    public record ElectionBoard(List<ElectionMemberItem> members, List<MandateCount> mandates, Instant computedAt) {}
    public record ElectionMemberItem(
            String memberSlug,
            String memberName,
            String factionShortName,
            String factionColorHex,
            int personalVotes,
            String mandateType,
            String partyName
    ) {}
    public record MandateCount(String mandateType, int count) {}

    /* ================ PARTY FINANCE (money in politics) ================ */
    public record PartyFinanceBoard(List<PartyFinanceItem> parties, int sinceYear, Instant computedAt) {}
    public record PartyFinanceItem(String partyName, String colorHex, long total, List<FinanceBucket> buckets) {}
    public record FinanceBucket(String key, long amount) {}

    /* ================ MP TOPIC RADAR ================ */
    public record MpTopicRadar(String slug, List<TopicSlice> topics, int totalBills) {}

    /* ================ NIGHT VOTES ================ */
    /**
     * "Night" votes are ones started outside conventional working hours
     * (before 08:00 or after 22:00 Europe/Tallinn by default).
     * Also flags weekend votes (Sat, Sun).
     */
    public record NightVotes(
            int totalVotes,
            int nightVotes,
            int weekendVotes,
            int lateNightVotes,       // strictly after 22:00 or before 06:00
            double nightRatio,        // 0..1
            int windowStartHour,      // e.g. 8
            int windowEndHour,        // e.g. 20
            List<NightVoteItem> items,
            List<NightHourBucket> hourDistribution,
            Instant computedAt
    ) {}
    public record NightVoteItem(
            UUID voteId,
            Integer voteNumber,
            String description,
            String startedAt,        // ISO in Europe/Tallinn
            int hourOfDay,
            int dayOfWeek,           // 1=Mon..7=Sun
            boolean weekend,
            boolean lateNight,
            int forCount,
            int againstCount,
            int margin,
            UUID linkedBillId,
            String linkedBillTitle
    ) {}
    public record NightHourBucket(int hourOfDay, int total, int weekday, int weekend) {}

    /* ================ MP DEVIATIONS TIMELINE ================ */
    /**
     * Per-day breakdown of a single MP's deviations from their faction's majority.
     * Renders as a GitHub-style contribution calendar on the profile.
     */
    public record MpDeviationsTimeline(
            String slug,
            List<DeviationDayCell> days,
            int totalDeviations,
            int totalEligible,
            java.time.LocalDate rangeFrom,
            java.time.LocalDate rangeTo
    ) {}
    public record DeviationDayCell(
            java.time.LocalDate date,
            int deviations,
            int eligible
    ) {}

    /* ================ MP SIMILAR PEERS ================ */
    /**
     * Top-N most similar and most opposite peers for a given MP,
     * ranked by voting agreement rate over their shared comparable votes.
     */
    public record MpSimilarPeers(
            String slug,
            List<PeerAgreement> mostSimilar,
            List<PeerAgreement> mostOpposite,
            int minOverlap,
            Instant computedAt
    ) {}
    public record PeerAgreement(
            String slug,
            String name,
            String factionShortName,
            String factionColorHex,
            double agreementRate,
            int overlap,
            int sameCount,
            int diffCount
    ) {}
}
