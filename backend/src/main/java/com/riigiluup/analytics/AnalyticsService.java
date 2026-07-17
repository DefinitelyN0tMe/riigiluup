package com.riigiluup.analytics;

import com.riigiluup.activity.MemberActivity;
import com.riigiluup.activity.MemberActivityRepository;
import com.riigiluup.election.ElectionResult;
import com.riigiluup.election.ElectionResultRepository;
import com.riigiluup.finance.PartyReceipt;
import com.riigiluup.finance.PartyReceiptRepository;
import com.riigiluup.party.Party;
import com.riigiluup.party.PartyRepository;
import com.riigiluup.group.Group;
import com.riigiluup.group.GroupRepository;
import com.riigiluup.group.GroupType;
import com.riigiluup.legislation.LegislationPhase;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.vote.IndividualVote;
import com.riigiluup.vote.VoteChoice;
import com.riigiluup.vote.VoteEventType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * All /api/v1/analytics/** aggregations.
 * <p>
 * Queries prefer native SQL where PostgreSQL date-part / bulk-aggregation is significantly faster
 * than JPQL; otherwise JPQL with typed Tuples.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final ZoneId TALLINN = ZoneId.of("Europe/Tallinn");

    @PersistenceContext
    private final EntityManager em;
    private final GroupRepository groupRepo;
    private final PlenaryMemberRepository memberRepo;
    private final MemberActivityRepository memberActivityRepo;
    private final ElectionResultRepository electionResultRepo;
    private final PartyReceiptRepository partyReceiptRepo;
    private final PartyRepository partyRepo;
    private final com.riigiluup.question.GovernmentQuestionRepository governmentQuestionRepo;

    /**
     * Faction name substrings currently in the governing coalition. Drives the
     * coalition/opposition axis of the MP scatter — update on any change of government
     * (config: riigiluup.analytics.coalition-factions). As of 2025 SDE left the coalition.
     */
    @Value("${riigiluup.analytics.coalition-factions:reform,eesti 200}")
    private List<String> coalitionFactionNames;

    /* ============================================================
     *  Faction-agreement matrix
     * ============================================================ */
    @Cacheable("analytics-faction-agreement")
    public AnalyticsDto.FactionAgreementMatrix factionAgreement(Instant from, Instant to) {
        List<Group> factions = activeFactions();
        int n = factions.size();
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(factions.get(i).getExternalId(), i);

        // Seats per faction
        Map<String, Integer> seats = seatsPerFaction();

        List<AnalyticsDto.FactionCell> cells = new ArrayList<>(n);
        for (Group g : factions) {
            cells.add(new AnalyticsDto.FactionCell(
                    g.getExternalId(),
                    g.getName(),
                    shortenFactionName(g.getName()),
                    g.getColorHex(),
                    seats.getOrDefault(g.getExternalId(), 0)
            ));
        }

        // Bulk-fetch pairwise majority-agreement counts
        String sql = """
            SELECT
              a.faction_external_id  AS fa,
              b.faction_external_id  AS fb,
              SUM(CASE WHEN a.majority_choice = b.majority_choice THEN 1 ELSE 0 END) AS same,
              COUNT(*) AS total
            FROM vote_faction_alignment a
            JOIN vote_faction_alignment b ON b.vote_event_id = a.vote_event_id
            JOIN vote_event ve             ON ve.id = a.vote_event_id
            WHERE a.has_clear_majority = TRUE
              AND b.has_clear_majority = TRUE
              AND a.majority_choice IN ('FOR', 'AGAINST', 'ABSTAINED')
              AND b.majority_choice IN ('FOR', 'AGAINST', 'ABSTAINED')
              AND (cast(:fromTs AS timestamp) IS NULL OR ve.started_at >= :fromTs)
              AND (cast(:toTs   AS timestamp) IS NULL OR ve.started_at <= :toTs)
            GROUP BY a.faction_external_id, b.faction_external_id
            """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("fromTs", from)
                .setParameter("toTs", to)
                .getResultList();

        // Init matrix with nulls (Java List of List of Double)
        List<List<Double>> matrix = new ArrayList<>(n);
        List<List<Integer>> support = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            List<Double> row = new ArrayList<>(n);
            List<Integer> supRow = new ArrayList<>(n);
            for (int j = 0; j < n; j++) { row.add(null); supRow.add(0); }
            matrix.add(row);
            support.add(supRow);
        }
        int totalVotes = 0;
        for (Object[] r : rows) {
            String fa = (String) r[0];
            String fb = (String) r[1];
            long same = ((Number) r[2]).longValue();
            long total = ((Number) r[3]).longValue();
            Integer i = idx.get(fa);
            Integer j = idx.get(fb);
            if (i == null || j == null || total == 0) continue;
            double rate = (double) same / (double) total;
            matrix.get(i).set(j, rate);
            support.get(i).set(j, (int) total);
            if (i.equals(j)) totalVotes = Math.max(totalVotes, (int) total);
        }
        return new AnalyticsDto.FactionAgreementMatrix(cells, matrix, support, totalVotes, Instant.now());
    }

    /* ============================================================
     *  Discipline breakers
     * ============================================================ */
    @Cacheable("analytics-discipline-breakers")
    public AnalyticsDto.DisciplineBreakers disciplineBreakers(int limit, int minEligible) {
        String jpql = """
            SELECT
                iv.plenaryMember,
                SUM(CASE WHEN iv.choice <> a.majorityChoice THEN 1 ELSE 0 END),
                COUNT(iv)
            FROM IndividualVote iv
            JOIN VoteFactionAlignment a
              ON a.voteEvent = iv.voteEvent
             AND a.factionExternalId = iv.factionExternalId
            WHERE a.hasClearMajority = TRUE
              AND iv.choice IN (
                    com.riigiluup.vote.VoteChoice.FOR,
                    com.riigiluup.vote.VoteChoice.AGAINST,
                    com.riigiluup.vote.VoteChoice.ABSTAINED)
              AND a.majorityChoice IN (
                    com.riigiluup.vote.VoteChoice.FOR,
                    com.riigiluup.vote.VoteChoice.AGAINST,
                    com.riigiluup.vote.VoteChoice.ABSTAINED)
            GROUP BY iv.plenaryMember
            HAVING COUNT(iv) >= :min
            """;
        List<Tuple> rows = em.createQuery(jpql, Tuple.class)
                .setParameter("min", (long) minEligible)
                .getResultList();

        // Sort by rate desc client-side (safer than JPQL HAVING/ORDER combos)
        record Row(PlenaryMember m, int devs, int elig, double rate) {}
        List<Row> ranked = rows.stream()
                .map(t -> {
                    PlenaryMember m = t.get(0, PlenaryMember.class);
                    long devs = ((Number) t.get(1)).longValue();
                    long elig = ((Number) t.get(2)).longValue();
                    double rate = elig == 0 ? 0 : (double) devs / (double) elig;
                    return new Row(m, (int) devs, (int) elig, rate);
                })
                .sorted(Comparator.<Row>comparingDouble(r -> -r.rate).thenComparingInt(r -> -r.devs))
                .limit(limit)
                .toList();

        // Faction colors
        Map<String, Group> factionByExt = activeFactions().stream()
                .collect(Collectors.toMap(Group::getExternalId, g -> g, (a, b) -> a));

        // Fetch 1 example deviation per top MP (batched via loop; small N=limit)
        List<AnalyticsDto.DisciplineBreaker> items = new ArrayList<>(ranked.size());
        for (Row r : ranked) {
            IndividualVote example = fetchOneRecentDeviation(r.m);
            Group faction = r.m.getFactionExternalId() != null ? factionByExt.get(r.m.getFactionExternalId()) : null;
            String factionName = r.m.getFactionName();
            String factionShort = faction != null ? shortenFactionName(faction.getName()) : shortenFactionName(factionName);
            String colorHex = faction != null ? faction.getColorHex() : null;
            items.add(new AnalyticsDto.DisciplineBreaker(
                    r.m.getSlug(), r.m.getFullName(), r.m.getFactionExternalId(),
                    factionName, factionShort, colorHex,
                    r.devs, r.elig, r.rate,
                    example != null ? example.getVoteEvent().getDescription() : null,
                    example != null && example.getVoteEvent().getStartedAt() != null
                            ? example.getVoteEvent().getStartedAt().toString() : null,
                    example != null ? example.getVoteEvent().getId() : null
            ));
        }
        return new AnalyticsDto.DisciplineBreakers(items, Instant.now());
    }

    /* ============================================================
     *  Most active MPs — speeches, questions, interpellations, written questions
     * ============================================================ */
    @Cacheable("analytics-member-activity")
    public AnalyticsDto.MemberActivityBoard memberActivity() {
        Map<String, MemberActivity> byExt = memberActivityRepo.findAll().stream()
                .collect(Collectors.toMap(MemberActivity::getMemberExternalId, a -> a, (a, b) -> a));
        Map<String, Group> factionByExt = activeFactions().stream()
                .collect(Collectors.toMap(Group::getExternalId, g -> g, (a, b) -> a));

        List<AnalyticsDto.MemberActivityItem> items = new ArrayList<>();
        for (PlenaryMember m : memberRepo.findAll()) {
            if (!m.isActive()) continue;
            MemberActivity a = byExt.get(m.getExternalId());
            if (a == null) continue;
            Group faction = m.getFactionExternalId() != null ? factionByExt.get(m.getFactionExternalId()) : null;
            String factionShort = faction != null ? shortenFactionName(faction.getName())
                    : shortenFactionName(m.getFactionName());
            String colorHex = faction != null ? faction.getColorHex() : null;
            items.add(new AnalyticsDto.MemberActivityItem(
                    m.getSlug(), m.getFullName(), factionShort, colorHex,
                    a.getSpeeches(), a.getQuestions(), a.getInterpellations(), a.getWrittenQuestions()));
        }
        return new AnalyticsDto.MemberActivityBoard(items, Instant.now());
    }

    /* ============================================================
     *  Elections — personal votes and mandate type of sitting MPs (RK_2023)
     * ============================================================ */
    @Cacheable("analytics-elections")
    public AnalyticsDto.ElectionBoard elections() {
        Map<String, ElectionResult> byExt = electionResultRepo.findAll().stream()
                .collect(Collectors.toMap(ElectionResult::getMemberExternalId, e -> e, (a, b) -> a));
        Map<String, Group> factionByExt = activeFactions().stream()
                .collect(Collectors.toMap(Group::getExternalId, g -> g, (a, b) -> a));

        List<AnalyticsDto.ElectionMemberItem> items = new ArrayList<>();
        Map<String, Integer> mandateCounts = new LinkedHashMap<>();
        for (PlenaryMember m : memberRepo.findAll()) {
            if (!m.isActive()) continue;
            ElectionResult e = byExt.get(m.getExternalId());
            if (e == null) continue;
            Group faction = m.getFactionExternalId() != null ? factionByExt.get(m.getFactionExternalId()) : null;
            String factionShort = faction != null ? shortenFactionName(faction.getName())
                    : shortenFactionName(m.getFactionName());
            String colorHex = faction != null ? faction.getColorHex() : null;
            items.add(new AnalyticsDto.ElectionMemberItem(
                    m.getSlug(), m.getFullName(), factionShort, colorHex,
                    e.getPersonalVotes(), e.getMandateType(), e.getPartyName()));
            mandateCounts.merge(e.getMandateType(), 1, Integer::sum);
        }
        items.sort(Comparator.comparingInt(AnalyticsDto.ElectionMemberItem::personalVotes).reversed());

        List<AnalyticsDto.MandateCount> mandates = mandateCounts.entrySet().stream()
                .map(en -> new AnalyticsDto.MandateCount(en.getKey(), en.getValue()))
                .toList();
        return new AnalyticsDto.ElectionBoard(items, mandates, Instant.now());
    }

    /* ============================================================
     *  Government response latency — interpellations + written questions
     * ============================================================ */

    /**
     * How long ministers take to answer, measured against the legal deadline the source
     * itself provides. Current term only; ministers with fewer than 5 questions are
     * dropped so a single late answer can't dominate the board.
     */
    @Cacheable("analytics-response-latency")
    public AnalyticsDto.ResponseLatencyBoard responseLatency() {
        LocalDate since = com.riigiluup.statistics.StatisticsService.TERM_START;
        List<AnalyticsDto.ResponseLatencyItem> items = governmentQuestionRepo
                .latencyByAddressee(since, 5).stream()
                .map(r -> new AnalyticsDto.ResponseLatencyItem(
                        r.getAddresseeName(), r.getAddresseeRole(),
                        r.getTotal(), r.getAnswered(), r.getAnsweredOnTime(),
                        r.getMedianDaysToAnswer(), r.getOverdueNow()))
                .toList();
        return new AnalyticsDto.ResponseLatencyBoard(items, since, Instant.now());
    }

    /* ============================================================
     *  Party finance — income mix of the parliamentary parties (ERJK)
     * ============================================================ */
    private static final int FINANCE_SINCE_YEAR = 2023; // current term

    @Cacheable("analytics-party-finance")
    public AnalyticsDto.PartyFinanceBoard partyFinance() {
        List<PartyReceipt> receipts = partyReceiptRepo.findAll().stream()
                .filter(r -> r.getPeriodYear() >= FINANCE_SINCE_YEAR)
                .toList();

        List<AnalyticsDto.PartyFinanceItem> items = new ArrayList<>();
        for (Party p : partyRepo.findAll()) {
            String pname = p.getFullName() == null ? "" : p.getFullName().toLowerCase(java.util.Locale.ROOT).trim();
            if (pname.isEmpty()) continue;

            // ERJK party names carry the same core party name (sometimes with a prefix), so a
            // contains-match on the lowercased name maps each of our parties to its ERJK rows.
            Map<String, Long> byBucket = new LinkedHashMap<>();
            for (PartyReceipt r : receipts) {
                if (r.getPartyName() == null) continue;
                if (!r.getPartyName().toLowerCase(java.util.Locale.ROOT).contains(pname)) continue;
                byBucket.merge(bucketOf(r.getCategoryId()), r.getAmount().longValue(), Long::sum);
            }
            long total = byBucket.values().stream().mapToLong(Long::longValue).sum();
            if (total <= 0) continue;

            List<AnalyticsDto.FinanceBucket> buckets = byBucket.entrySet().stream()
                    .map(e -> new AnalyticsDto.FinanceBucket(e.getKey(), e.getValue()))
                    .sorted(Comparator.comparingLong(AnalyticsDto.FinanceBucket::amount).reversed())
                    .toList();
            items.add(new AnalyticsDto.PartyFinanceItem(p.getFullName(), p.getColorHex(), total, buckets));
        }
        items.sort(Comparator.comparingLong(AnalyticsDto.PartyFinanceItem::total).reversed());
        return new AnalyticsDto.PartyFinanceBoard(items, FINANCE_SINCE_YEAR, Instant.now());
    }

    /** Collapse the seven ERJK income types into a few readable buckets. */
    private static String bucketOf(String categoryId) {
        return switch (categoryId == null ? "" : categoryId) {
            case "113" -> "state";              // Riigitoetus (state support)
            case "111", "112" -> "donations";   // Rahaline + Mitterahaline annetus
            case "110" -> "membership";         // Liikmemaks (membership fees)
            case "115" -> "loans";              // Pangalaen (bank loan)
            default -> "other";                 // 116 Isiklikud vahendid, 114 Tulu erakonna varalt
        };
    }

    private IndividualVote fetchOneRecentDeviation(PlenaryMember m) {
        String jpql = """
            SELECT iv FROM IndividualVote iv
            JOIN VoteFactionAlignment a
              ON a.voteEvent = iv.voteEvent
             AND a.factionExternalId = iv.factionExternalId
            WHERE iv.plenaryMember = :m
              AND a.hasClearMajority = TRUE
              AND iv.choice IN (
                    com.riigiluup.vote.VoteChoice.FOR,
                    com.riigiluup.vote.VoteChoice.AGAINST,
                    com.riigiluup.vote.VoteChoice.ABSTAINED)
              AND iv.choice <> a.majorityChoice
            ORDER BY iv.voteEvent.startedAt DESC
            """;
        return em.createQuery(jpql, IndividualVote.class)
                .setParameter("m", m)
                .setMaxResults(1)
                .getResultStream()
                .findFirst().orElse(null);
    }

    /* ============================================================
     *  Bill flow (Sankey)
     * ============================================================ */
    @Cacheable("analytics-bill-flow")
    public AnalyticsDto.BillFlow billFlow() {
        // Phase counts as terminal nodes
        String jpql = "SELECT li.phase, COUNT(li) FROM LegislativeItem li GROUP BY li.phase";
        List<Tuple> phaseRows = em.createQuery(jpql, Tuple.class).getResultList();
        Map<LegislationPhase, Long> byPhase = new LinkedHashMap<>();
        long total = 0;
        for (Tuple t : phaseRows) {
            byPhase.put(t.get(0, LegislationPhase.class), t.get(1, Long.class));
            total += t.get(1, Long.class);
        }

        // Reading counts from stages
        String stageSql = """
            SELECT reading_code, COUNT(DISTINCT legislative_item_id)
            FROM legislative_stage
            WHERE reading_code IS NOT NULL
            GROUP BY reading_code
            """;
        List<Object[]> stageRows = em.createNativeQuery(stageSql).getResultList();
        Map<String, Long> byReading = new LinkedHashMap<>();
        for (Object[] r : stageRows) {
            byReading.put((String) r[0], ((Number) r[1]).longValue());
        }

        // Build a simple 4-column Sankey: initiated → readings → outcome
        long initiated = total;
        long firstR = byReading.getOrDefault("ESIMENE_LUGEMINE", 0L) + byReading.getOrDefault("FIRST_READING", 0L);
        long secondR = byReading.getOrDefault("TEINE_LUGEMINE", 0L) + byReading.getOrDefault("SECOND_READING", 0L);
        long thirdR = byReading.getOrDefault("KOLMAS_LUGEMINE", 0L) + byReading.getOrDefault("THIRD_READING", 0L);
        long adopted = byPhase.getOrDefault(LegislationPhase.ADOPTED, 0L);
        long rejected = byPhase.getOrDefault(LegislationPhase.REJECTED, 0L);
        long withdrawn = byPhase.getOrDefault(LegislationPhase.WITHDRAWN, 0L);
        long inCommittee = byPhase.getOrDefault(LegislationPhase.IN_COMMITTEE, 0L);
        long inReadings = byPhase.getOrDefault(LegislationPhase.IN_READINGS, 0L);
        long submitted = byPhase.getOrDefault(LegislationPhase.SUBMITTED, 0L);
        long other = byPhase.getOrDefault(LegislationPhase.OTHER, 0L);

        List<AnalyticsDto.BillFlowNode> nodes = List.of(
                new AnalyticsDto.BillFlowNode("initiated", "Algatatud", (int) initiated),
                new AnalyticsDto.BillFlowNode("in_committee", "Komisjonis", (int) inCommittee),
                new AnalyticsDto.BillFlowNode("first_reading", "I lugemine", (int) firstR),
                new AnalyticsDto.BillFlowNode("second_reading", "II lugemine", (int) secondR),
                new AnalyticsDto.BillFlowNode("third_reading", "III lugemine", (int) thirdR),
                new AnalyticsDto.BillFlowNode("in_readings", "Lugemistel", (int) inReadings),
                new AnalyticsDto.BillFlowNode("submitted", "Esitatud", (int) submitted),
                new AnalyticsDto.BillFlowNode("adopted", "Vastu võetud", (int) adopted),
                new AnalyticsDto.BillFlowNode("rejected", "Tagasi lükatud", (int) rejected),
                new AnalyticsDto.BillFlowNode("withdrawn", "Tagasi võetud", (int) withdrawn),
                new AnalyticsDto.BillFlowNode("other", "Muu", (int) other)
        );

        List<AnalyticsDto.BillFlowLink> links = new ArrayList<>();
        // initiated → phase buckets
        addLink(links, "initiated", "in_committee", inCommittee);
        addLink(links, "initiated", "first_reading", firstR);
        addLink(links, "initiated", "submitted", submitted);
        addLink(links, "initiated", "other", other);
        // readings → outcomes (approximation: distribute readings across outcomes proportionally)
        long readingsTotal = Math.max(firstR + secondR + thirdR + inReadings, 1);
        long adoptShare = Math.max(adopted, 0);
        long rejectShare = Math.max(rejected, 0);
        long withdrawShare = Math.max(withdrawn, 0);
        addLink(links, "first_reading", "second_reading", secondR);
        addLink(links, "second_reading", "third_reading", thirdR);
        addLink(links, "third_reading", "adopted", adoptShare);
        addLink(links, "first_reading", "rejected", rejectShare);
        addLink(links, "first_reading", "withdrawn", withdrawShare);
        addLink(links, "in_committee", "in_readings", inReadings);

        return new AnalyticsDto.BillFlow(nodes, links, (int) total, Instant.now());
    }
    private void addLink(List<AnalyticsDto.BillFlowLink> links, String s, String t, long w) {
        if (w > 0) links.add(new AnalyticsDto.BillFlowLink(s, t, (int) w));
    }

    /* ============================================================
     *  Attendance matrix
     * ============================================================ */
    @Cacheable("analytics-attendance-matrix")
    public AnalyticsDto.AttendanceMatrix attendanceMatrix(int recentSittings) {
        // Column set: last N attendance-check vote events, ordered oldest→newest
        String colSql = """
            SELECT id, sitting_external_id, sitting_title, started_at
            FROM vote_event
            WHERE type = 'ATTENDANCE_CHECK' AND started_at IS NOT NULL
            ORDER BY started_at DESC
            LIMIT :n
            """;
        List<Object[]> colRows = em.createNativeQuery(colSql)
                .setParameter("n", recentSittings)
                .getResultList();
        // Reverse to chronological
        List<Object[]> chronological = new ArrayList<>(colRows);
        java.util.Collections.reverse(chronological);

        List<AnalyticsDto.AttendanceCol> cols = new ArrayList<>();
        List<UUID> colIds = new ArrayList<>();
        Map<UUID, Integer> colIdx = new HashMap<>();
        for (int i = 0; i < chronological.size(); i++) {
            Object[] r = chronological.get(i);
            UUID id = uuidOf(r[0]);
            String sitting = (String) r[1];
            Instant ts = instantOf(r[3]);
            String date = ts == null ? "" : ts.atZone(TALLINN).toLocalDate().toString();
            String label = date.substring(5); // MM-DD
            cols.add(new AnalyticsDto.AttendanceCol(sitting, date, label));
            colIds.add(id);
            colIdx.put(id, i);
        }
        int cN = cols.size();

        // Rows: active MPs
        List<PlenaryMember> members = memberRepo.findByActiveTrueOrderByLastNameAscFirstNameAsc();
        int mN = members.size();
        Map<UUID, Integer> memberIdx = new HashMap<>();
        for (int i = 0; i < mN; i++) memberIdx.put(members.get(i).getId(), i);

        // Fetch all individual_vote for those vote-events + members
        String cellSql = """
            SELECT plenary_member_id, vote_event_id, choice
            FROM individual_vote
            WHERE vote_event_id IN (:ids)
            """;
        @SuppressWarnings("unchecked")
        List<Object[]> cellRows = colIds.isEmpty()
                ? List.of()
                : em.createNativeQuery(cellSql).setParameter("ids", colIds).getResultList();

        // Initialize with "-"
        String[] flat = new String[Math.max(mN * cN, 1)];
        Arrays.fill(flat, "-");
        int[] presentByMember = new int[mN];
        int[] recordedByMember = new int[mN]; // per-MP denominator: checks where the MP had *any* record

        for (Object[] r : cellRows) {
            UUID mid = uuidOf(r[0]);
            UUID vid = uuidOf(r[1]);
            String choice = String.valueOf(r[2]);
            Integer mi = memberIdx.get(mid);
            Integer ci = colIdx.get(vid);
            if (mi == null || ci == null) continue;
            boolean present = "PRESENT".equals(choice) || "FOR".equals(choice) || "AGAINST".equals(choice) || "ABSTAINED".equals(choice);
            flat[mi * cN + ci] = present ? "P" : "A";
            recordedByMember[mi]++;
            if (present) presentByMember[mi]++;
        }

        // Faction color map
        Map<String, Group> factionByExt = activeFactions().stream()
                .collect(Collectors.toMap(Group::getExternalId, g -> g, (a, b) -> a));

        // Drop MPs who were not seated during any of the recent N checks — otherwise a
        // 0/0 row is meaningless noise and would also blow up the per-MP percentage.
        int minChecksToInclude = Math.max(1, cN / 5);
        List<AnalyticsDto.AttendanceRow> rows = new ArrayList<>();
        // Rebuild flat cells to match the filtered member order.
        List<String> filteredCells = new ArrayList<>();
        for (int i = 0; i < mN; i++) {
            if (recordedByMember[i] < minChecksToInclude) continue;
            PlenaryMember m = members.get(i);
            Group f = m.getFactionExternalId() != null ? factionByExt.get(m.getFactionExternalId()) : null;
            String factionShort = f != null ? shortenFactionName(f.getName()) : shortenFactionName(m.getFactionName());
            String factionColor = f != null ? f.getColorHex() : null;
            rows.add(new AnalyticsDto.AttendanceRow(m.getSlug(), m.getLastName(), factionShort, factionColor,
                    presentByMember[i], recordedByMember[i]));
            for (int c = 0; c < cN; c++) filteredCells.add(flat[i * cN + c]);
        }
        return new AnalyticsDto.AttendanceMatrix(rows, cols, filteredCells, rows.size(), cN);
    }

    /* ============================================================
     *  Vote timing heatmap
     * ============================================================ */
    @Cacheable("analytics-vote-timing")
    public AnalyticsDto.VoteTimingHeatmap voteTiming() {
        // Postgres: dow returns 0=Sun..6=Sat; we want Mon(0)..Sun(6). Use MOD(); % is a JPA reserved char.
        String sql = """
            SELECT
              MOD(EXTRACT(dow FROM (started_at AT TIME ZONE 'Europe/Tallinn'))::int + 6, 7) AS dow_mon,
              EXTRACT(hour FROM (started_at AT TIME ZONE 'Europe/Tallinn'))::int AS hr,
              COUNT(*)
            FROM vote_event
            WHERE started_at IS NOT NULL
            GROUP BY 1, 2
            """;
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        int[][] cells = new int[7][24];
        int total = 0, max = 0;
        for (Object[] r : rows) {
            int dow = ((Number) r[0]).intValue();
            int hr = ((Number) r[1]).intValue();
            int c = ((Number) r[2]).intValue();
            if (dow < 0 || dow > 6 || hr < 0 || hr > 23) continue;
            cells[dow][hr] = c;
            total += c;
            if (c > max) max = c;
        }
        List<List<Integer>> out = new ArrayList<>(7);
        for (int d = 0; d < 7; d++) {
            List<Integer> row = new ArrayList<>(24);
            for (int h = 0; h < 24; h++) row.add(cells[d][h]);
            out.add(row);
        }
        return new AnalyticsDto.VoteTimingHeatmap(out, total, max);
    }

    /* ============================================================
     *  Topic treemap
     * ============================================================ */
    @Cacheable("analytics-topic-treemap")
    public AnalyticsDto.TopicTreemap topicTreemap(int limit) {
        String sql = """
            SELECT t.edid, t.text,
                   COUNT(DISTINCT lit.legislative_item_id) AS billn,
                   SUM(CASE WHEN li.phase = 'ADOPTED' THEN 1 ELSE 0 END) AS adoptedn
            FROM topic t
            JOIN legislative_item_topic lit ON lit.topic_id = t.id
            JOIN legislative_item li ON li.id = lit.legislative_item_id
            GROUP BY t.edid, t.text
            ORDER BY billn DESC
            LIMIT :n
            """;
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("n", limit)
                .getResultList();
        List<AnalyticsDto.TopicSlice> items = new ArrayList<>(rows.size());
        int totalBills = 0;
        for (Object[] r : rows) {
            int edid = ((Number) r[0]).intValue();
            String label = (String) r[1];
            int bills = ((Number) r[2]).intValue();
            int adopted = ((Number) r[3]).intValue();
            totalBills += bills;
            items.add(new AnalyticsDto.TopicSlice(edid, label, bills, adopted));
        }
        return new AnalyticsDto.TopicTreemap(items, totalBills);
    }

    /* ============================================================
     *  Bill velocity histogram
     * ============================================================ */
    @Cacheable("analytics-bill-velocity")
    public AnalyticsDto.BillVelocity billVelocity() {
        String sql = """
            SELECT (accepted_date - initiated_date) AS days
            FROM legislative_item
            WHERE phase = 'ADOPTED' AND accepted_date IS NOT NULL AND initiated_date IS NOT NULL
            """;
        List<Object> rows = em.createNativeQuery(sql).getResultList();
        List<Integer> days = rows.stream()
                .map(o -> ((Number) o).intValue())
                .filter(d -> d >= 0)
                .sorted()
                .toList();

        int total = days.size();
        int median = total == 0 ? 0 : days.get(total / 2);
        int p90 = total == 0 ? 0 : days.get(Math.min(total - 1, (int) Math.floor(total * 0.9)));
        int fastest = total == 0 ? 0 : days.get(0);
        int slowest = total == 0 ? 0 : days.get(total - 1);

        // Bucket: 0-7, 8-30, 31-90, 91-180, 181-365, 365+
        int[] boundaries = {7, 30, 90, 180, 365, Integer.MAX_VALUE};
        String[] labels = {"≤ 1 näd", "≤ 1 kuu", "≤ 3 kuud", "≤ 6 kuud", "≤ 1 aasta", "> 1 aasta"};
        int[] counts = new int[boundaries.length];
        for (Integer d : days) {
            for (int i = 0; i < boundaries.length; i++) {
                if (d <= boundaries[i]) { counts[i]++; break; }
            }
        }
        List<AnalyticsDto.VelocityBucket> buckets = new ArrayList<>(boundaries.length);
        for (int i = 0; i < boundaries.length; i++) {
            buckets.add(new AnalyticsDto.VelocityBucket(labels[i], boundaries[i], counts[i]));
        }
        return new AnalyticsDto.BillVelocity(buckets, total, median, p90, fastest, slowest);
    }

    /* ============================================================
     *  MP similarity scatter
     * ============================================================ */
    @Cacheable("analytics-mp-similarity")
    public AnalyticsDto.MpSimilarity mpSimilarity() {
        // For each MP compute:
        //   x = (agree-with-coalition-majority %) - (agree-with-opposition-majority %) in [-1,1]
        //   y = 2 * group-alignment-rate - 1  in [-1,1]  (party loyalty axis)
        Map<String, String> partyRole = coalitionRoleMap();

        // Bulk fetch per-MP alignment rate against every faction's majority (only where has_clear_majority)
        String sql = """
            SELECT
              iv.plenary_member_id AS mid,
              a.faction_external_id AS fex,
              SUM(CASE WHEN iv.choice = a.majority_choice THEN 1 ELSE 0 END) AS agree,
              COUNT(*) AS total
            FROM individual_vote iv
            JOIN vote_faction_alignment a
              ON a.vote_event_id = iv.vote_event_id
             AND a.has_clear_majority = TRUE
             AND a.majority_choice IN ('FOR','AGAINST','ABSTAINED')
            WHERE iv.choice IN ('FOR','AGAINST','ABSTAINED')
            GROUP BY iv.plenary_member_id, a.faction_external_id
            """;
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        // Aggregate per member: coalition vs opposition
        Map<UUID, double[]> memberScore = new HashMap<>();
        Map<UUID, int[]> memberCount = new HashMap<>();
        for (Object[] r : rows) {
            UUID mid = uuidOf(r[0]);
            String fex = (String) r[1];
            long agree = ((Number) r[2]).longValue();
            long total = ((Number) r[3]).longValue();
            if (total == 0) continue;
            String role = partyRole.get(fex);
            if (role == null) continue;
            double[] agg = memberScore.computeIfAbsent(mid, k -> new double[]{0, 0});
            int[] cnt = memberCount.computeIfAbsent(mid, k -> new int[]{0, 0});
            double rate = (double) agree / total;
            if ("coalition".equals(role)) { agg[0] += rate; cnt[0]++; }
            else if ("opposition".equals(role)) { agg[1] += rate; cnt[1]++; }
        }

        // Bulk fetch group alignment per MP
        String gaSql = """
            SELECT
              iv.plenary_member_id AS mid,
              SUM(CASE WHEN iv.choice = a.majority_choice THEN 1 ELSE 0 END) AS matches,
              SUM(CASE WHEN iv.choice <> a.majority_choice THEN 1 ELSE 0 END) AS devs
            FROM individual_vote iv
            JOIN vote_faction_alignment a
              ON a.vote_event_id = iv.vote_event_id
             AND a.faction_external_id = iv.faction_external_id
             AND a.has_clear_majority = TRUE
             AND a.majority_choice IN ('FOR','AGAINST','ABSTAINED')
            WHERE iv.choice IN ('FOR','AGAINST','ABSTAINED')
            GROUP BY iv.plenary_member_id
            """;
        List<Object[]> gaRows = em.createNativeQuery(gaSql).getResultList();
        Map<UUID, double[]> gaByMember = new HashMap<>();  // [matches, devs]
        for (Object[] r : gaRows) {
            gaByMember.put(uuidOf(r[0]),
                    new double[]{((Number) r[1]).doubleValue(), ((Number) r[2]).doubleValue()});
        }

        // Members + faction colors
        List<PlenaryMember> members = memberRepo.findByActiveTrueOrderByLastNameAscFirstNameAsc();
        Map<String, Group> factionByExt = activeFactions().stream()
                .collect(Collectors.toMap(Group::getExternalId, g -> g, (a, b) -> a));

        List<AnalyticsDto.MpPoint> points = new ArrayList<>();
        for (PlenaryMember m : members) {
            double[] agg = memberScore.get(m.getId());
            int[] cnt = memberCount.get(m.getId());
            double x = 0;
            int totalComparable = 0;
            if (agg != null && cnt != null) {
                double coRate = cnt[0] > 0 ? agg[0] / cnt[0] : 0;
                double opRate = cnt[1] > 0 ? agg[1] / cnt[1] : 0;
                x = coRate - opRate;
            }
            double y = 0;
            int devs = 0;
            double[] ga = gaByMember.get(m.getId());
            if (ga != null) {
                double matches = ga[0];
                double d = ga[1];
                totalComparable = (int) (matches + d);
                devs = (int) d;
                double rate = totalComparable == 0 ? 0.5 : matches / (matches + d);
                y = 2 * rate - 1;
            }
            // Small deterministic jitter to prevent stacking
            double jitterX = deterministicJitter(m.getSlug(), 1);
            double jitterY = deterministicJitter(m.getSlug(), 2);
            x = clamp(x + jitterX * 0.04, -1.0, 1.0);
            y = clamp(y + jitterY * 0.04, -1.0, 1.0);

            Group f = m.getFactionExternalId() != null ? factionByExt.get(m.getFactionExternalId()) : null;
            String factionShort = f != null ? shortenFactionName(f.getName()) : shortenFactionName(m.getFactionName());
            String color = f != null ? f.getColorHex() : "#0072CE";

            points.add(new AnalyticsDto.MpPoint(
                    m.getSlug(), m.getFullName(), factionShort, color, x, y, totalComparable, devs
            ));
        }
        return new AnalyticsDto.MpSimilarity(points, Instant.now());
    }

    /* ============================================================
     *  Co-sponsorship network
     * ============================================================ */
    @Cacheable("analytics-co-sponsorship")
    public AnalyticsDto.CoSponsorship coSponsorship(int minWeight) {
        // Nodes: MPs who have sponsored at least 1 bill
        String nodeSql = """
            SELECT ls.plenary_member_id, COUNT(DISTINCT ls.legislative_item_id) AS billn
            FROM legislative_sponsorship ls
            WHERE ls.sponsor_kind = 'PLENARY_MEMBER' AND ls.plenary_member_id IS NOT NULL
            GROUP BY ls.plenary_member_id
            """;
        List<Object[]> nodeRows = em.createNativeQuery(nodeSql).getResultList();
        Map<UUID, Integer> billsPer = new HashMap<>();
        for (Object[] r : nodeRows) {
            billsPer.put(uuidOf(r[0]), ((Number) r[1]).intValue());
        }

        // Edges: pairs of MPs who co-sponsored the same bill
        String edgeSql = """
            SELECT a.plenary_member_id, b.plenary_member_id, COUNT(DISTINCT a.legislative_item_id)
            FROM legislative_sponsorship a
            JOIN legislative_sponsorship b ON a.legislative_item_id = b.legislative_item_id
            WHERE a.plenary_member_id IS NOT NULL AND b.plenary_member_id IS NOT NULL
              AND a.plenary_member_id < b.plenary_member_id
            GROUP BY a.plenary_member_id, b.plenary_member_id
            HAVING COUNT(DISTINCT a.legislative_item_id) >= :min
            """;
        List<Object[]> edgeRows = em.createNativeQuery(edgeSql)
                .setParameter("min", minWeight)
                .getResultList();

        // Build node list only for MPs that appear in edges (keeps the graph tight)
        Set<UUID> activeIds = new LinkedHashSet<>();
        List<AnalyticsDto.CoSponsorEdge> edges = new ArrayList<>();
        Map<UUID, String> slugById = new HashMap<>();
        Map<UUID, PlenaryMember> memById = new HashMap<>();

        List<PlenaryMember> allMembers = memberRepo.findByActiveTrueOrderByLastNameAscFirstNameAsc();
        for (PlenaryMember m : allMembers) { memById.put(m.getId(), m); slugById.put(m.getId(), m.getSlug()); }

        for (Object[] r : edgeRows) {
            UUID ai = uuidOf(r[0]);
            UUID bi = uuidOf(r[1]);
            int w = ((Number) r[2]).intValue();
            String as = slugById.get(ai);
            String bs = slugById.get(bi);
            if (as == null || bs == null) continue;
            activeIds.add(ai);
            activeIds.add(bi);
            edges.add(new AnalyticsDto.CoSponsorEdge(as, bs, w));
        }

        Map<String, Group> factionByExt = activeFactions().stream()
                .collect(Collectors.toMap(Group::getExternalId, g -> g, (a, b) -> a));
        List<AnalyticsDto.CoSponsorNode> nodes = new ArrayList<>();
        for (UUID id : activeIds) {
            PlenaryMember m = memById.get(id);
            if (m == null) continue;
            Group f = m.getFactionExternalId() != null ? factionByExt.get(m.getFactionExternalId()) : null;
            String factionShort = f != null ? shortenFactionName(f.getName()) : shortenFactionName(m.getFactionName());
            String color = f != null ? f.getColorHex() : "#0072CE";
            nodes.add(new AnalyticsDto.CoSponsorNode(
                    m.getSlug(), m.getFullName(), factionShort, color,
                    billsPer.getOrDefault(id, 0)
            ));
        }

        // Total bills that had ≥2 MP sponsors
        String cntSql = """
            SELECT COUNT(*) FROM (
              SELECT ls.legislative_item_id
              FROM legislative_sponsorship ls
              WHERE ls.sponsor_kind = 'PLENARY_MEMBER' AND ls.plenary_member_id IS NOT NULL
              GROUP BY ls.legislative_item_id
              HAVING COUNT(DISTINCT ls.plenary_member_id) >= 2
            ) x
            """;
        int totalConsidered = ((Number) em.createNativeQuery(cntSql).getSingleResult()).intValue();

        return new AnalyticsDto.CoSponsorship(nodes, edges, totalConsidered);
    }

    /* ============================================================
     *  Highlights bundle
     * ============================================================ */
    @Cacheable("analytics-highlights")
    public AnalyticsDto.HighlightsBundle highlights() {
        Map<String, Group> factionByExt = activeFactions().stream()
                .collect(Collectors.toMap(Group::getExternalId, g -> g, (a, b) -> a));

        // Streaks: recent attendance-check runs
        int recent = 8;
        String streakSql = """
            WITH recent_checks AS (
              SELECT id FROM vote_event
              WHERE type = 'ATTENDANCE_CHECK' AND started_at IS NOT NULL
              ORDER BY started_at DESC LIMIT :n
            )
            SELECT iv.plenary_member_id,
                   SUM(CASE WHEN iv.choice IN ('PRESENT','FOR','AGAINST','ABSTAINED') THEN 1 ELSE 0 END),
                   COUNT(*)
            FROM individual_vote iv
            JOIN recent_checks c ON c.id = iv.vote_event_id
            GROUP BY iv.plenary_member_id
            HAVING SUM(CASE WHEN iv.choice IN ('PRESENT','FOR','AGAINST','ABSTAINED') THEN 1 ELSE 0 END) = COUNT(*)
                   AND COUNT(*) = :n
            LIMIT 12
            """;
        List<Object[]> streakRows = em.createNativeQuery(streakSql)
                .setParameter("n", recent)
                .getResultList();
        List<AnalyticsDto.AttendanceStreak> streaks = new ArrayList<>();
        for (Object[] r : streakRows) {
            UUID mid = uuidOf(r[0]);
            PlenaryMember m = memberRepo.findById(mid).orElse(null);
            if (m == null) continue;
            Group f = m.getFactionExternalId() != null ? factionByExt.get(m.getFactionExternalId()) : null;
            String factionShort = f != null ? shortenFactionName(f.getName()) : shortenFactionName(m.getFactionName());
            streaks.add(new AnalyticsDto.AttendanceStreak(m.getSlug(), m.getFullName(), factionShort,
                    recent, recent));
        }

        // Tight votes: last 20 named votes with smallest FOR-AGAINST margin (both > 0)
        String tightSql = """
            SELECT id, voting_number, description, started_at, result_in_favor, result_against
            FROM vote_event
            WHERE type = 'OPEN' AND started_at IS NOT NULL
              AND result_in_favor > 0 AND result_against > 0
            ORDER BY ABS(result_in_favor - result_against) ASC
            LIMIT 6
            """;
        List<Object[]> tightRows = em.createNativeQuery(tightSql).getResultList();
        List<AnalyticsDto.VoteMargin> tightVotes = new ArrayList<>();
        for (Object[] r : tightRows) {
            UUID vid = uuidOf(r[0]);
            Integer num = r[1] == null ? null : ((Number) r[1]).intValue();
            String desc = (String) r[2];
            Instant ts = instantOf(r[3]);
            int forC = ((Number) r[4]).intValue();
            int agnC = ((Number) r[5]).intValue();
            tightVotes.add(new AnalyticsDto.VoteMargin(vid, num, desc,
                    ts != null ? ts.toString() : null,
                    forC, agnC, Math.abs(forC - agnC)));
        }

        return new AnalyticsDto.HighlightsBundle(streaks, tightVotes);
    }

    /* ============================================================
     *  Night votes — votes cast outside 08:00–22:00 Europe/Tallinn (default window)
     * ============================================================ */
    @Cacheable("analytics-night-votes")
    public AnalyticsDto.NightVotes nightVotes(int windowStartHour, int windowEndHour, int limit) {
        int startH = Math.max(0, Math.min(23, windowStartHour));
        int endH = Math.max(0, Math.min(24, windowEndHour));

        // Aggregate hour × dow (weekday vs weekend) for the histogram
        String histSql = """
            SELECT
              EXTRACT(hour FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int AS hr,
              MOD(EXTRACT(dow FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int + 6, 7) AS dow_mon,
              COUNT(*)
            FROM vote_event ve
            WHERE ve.started_at IS NOT NULL AND ve.type = 'OPEN'
            GROUP BY 1, 2
            """;
        List<Object[]> histRows = em.createNativeQuery(histSql).getResultList();
        int[] byHour = new int[24];
        int[] wkdByHour = new int[24];
        int[] wkeByHour = new int[24];
        int total = 0;
        for (Object[] r : histRows) {
            int hr = ((Number) r[0]).intValue();
            int dow = ((Number) r[1]).intValue();
            int c = ((Number) r[2]).intValue();
            if (hr < 0 || hr > 23) continue;
            byHour[hr] += c;
            if (dow >= 5) wkeByHour[hr] += c;
            else wkdByHour[hr] += c;
            total += c;
        }

        // Fetch actual night votes (with tallies) newest-first
        String listSql = """
            SELECT
              ve.id, ve.voting_number, ve.description, ve.started_at,
              EXTRACT(hour FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int,
              MOD(EXTRACT(dow FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int + 6, 7),
              ve.result_in_favor, ve.result_against,
              li.id, li.title
            FROM vote_event ve
            LEFT JOIN legislative_item li ON li.id = ve.legislative_item_id
            WHERE ve.started_at IS NOT NULL AND ve.type = 'OPEN'
              AND (
                EXTRACT(hour FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int < :startH
                OR EXTRACT(hour FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int >= :endH
                OR MOD(EXTRACT(dow FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int + 6, 7) >= 5
              )
            ORDER BY ve.started_at DESC
            LIMIT :n
            """;
        List<Object[]> listRows = em.createNativeQuery(listSql)
                .setParameter("startH", startH)
                .setParameter("endH", endH)
                .setParameter("n", limit)
                .getResultList();

        List<AnalyticsDto.NightVoteItem> items = new ArrayList<>();
        int nightTotal = 0, weekendTotal = 0, lateNightTotal = 0;
        for (Object[] r : listRows) {
            UUID id = uuidOf(r[0]);
            Integer num = r[1] == null ? null : ((Number) r[1]).intValue();
            String desc = (String) r[2];
            Instant startedAt = instantOf(r[3]);
            int hr = ((Number) r[4]).intValue();
            int dow = ((Number) r[5]).intValue();
            int forC = ((Number) r[6]).intValue();
            int agnC = ((Number) r[7]).intValue();
            UUID billId = uuidOf(r[8]);
            String billTitle = (String) r[9];
            boolean weekend = dow >= 5;
            boolean lateNight = hr >= 22 || hr < 6;
            if (weekend) weekendTotal++;
            if (lateNight) lateNightTotal++;
            nightTotal++;
            items.add(new AnalyticsDto.NightVoteItem(
                    id, num, desc,
                    startedAt == null ? null : startedAt.toString(),
                    hr, dow + 1,        // return 1..7 (Mon..Sun)
                    weekend, lateNight,
                    forC, agnC, Math.abs(forC - agnC),
                    billId, billTitle
            ));
        }

        // Full-corpus counts for night/weekend/lateNight (bulk query, not just limit)
        String countsSql = """
            SELECT
              SUM(CASE
                WHEN EXTRACT(hour FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int < :startH
                  OR EXTRACT(hour FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int >= :endH
                THEN 1 ELSE 0 END) AS nightN,
              SUM(CASE WHEN MOD(EXTRACT(dow FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int + 6, 7) >= 5
                THEN 1 ELSE 0 END) AS wknN,
              SUM(CASE
                WHEN EXTRACT(hour FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int >= 22
                  OR EXTRACT(hour FROM (ve.started_at AT TIME ZONE 'Europe/Tallinn'))::int < 6
                THEN 1 ELSE 0 END) AS lateN
            FROM vote_event ve
            WHERE ve.started_at IS NOT NULL AND ve.type = 'OPEN'
            """;
        Object[] c = (Object[]) em.createNativeQuery(countsSql)
                .setParameter("startH", startH)
                .setParameter("endH", endH)
                .getSingleResult();
        int nightAll = c[0] == null ? 0 : ((Number) c[0]).intValue();
        int wknAll = c[1] == null ? 0 : ((Number) c[1]).intValue();
        int lateAll = c[2] == null ? 0 : ((Number) c[2]).intValue();

        List<AnalyticsDto.NightHourBucket> hourDistribution = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            hourDistribution.add(new AnalyticsDto.NightHourBucket(h, byHour[h], wkdByHour[h], wkeByHour[h]));
        }
        double ratio = total == 0 ? 0.0 : (double) nightAll / (double) total;

        return new AnalyticsDto.NightVotes(
                total, nightAll, wknAll, lateAll, ratio,
                startH, endH,
                items, hourDistribution,
                Instant.now()
        );
    }

    /* ============================================================
     *  MP topic radar (per MP)
     * ============================================================ */
    @Cacheable("analytics-mp-topic-radar")
    public AnalyticsDto.MpTopicRadar mpTopicRadar(String slug, int limit) {
        String sql = """
            SELECT t.edid, t.text,
                   COUNT(DISTINCT lit.legislative_item_id) AS bn,
                   SUM(CASE WHEN li.phase = 'ADOPTED' THEN 1 ELSE 0 END) AS an
            FROM legislative_sponsorship ls
            JOIN plenary_member pm ON pm.id = ls.plenary_member_id
            JOIN legislative_item_topic lit ON lit.legislative_item_id = ls.legislative_item_id
            JOIN topic t ON t.id = lit.topic_id
            JOIN legislative_item li ON li.id = ls.legislative_item_id
            WHERE pm.slug = :slug AND ls.sponsor_kind = 'PLENARY_MEMBER'
            GROUP BY t.edid, t.text
            ORDER BY bn DESC
            LIMIT :n
            """;
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("slug", slug)
                .setParameter("n", limit)
                .getResultList();
        List<AnalyticsDto.TopicSlice> topics = new ArrayList<>();
        int total = 0;
        for (Object[] r : rows) {
            int edid = ((Number) r[0]).intValue();
            String label = (String) r[1];
            int bn = ((Number) r[2]).intValue();
            int an = ((Number) r[3]).intValue();
            total += bn;
            topics.add(new AnalyticsDto.TopicSlice(edid, label, bn, an));
        }
        return new AnalyticsDto.MpTopicRadar(slug, topics, total);
    }

    /* ============================================================
     *  MP deviations timeline
     * ============================================================ */
    @Cacheable("analytics-mp-deviations-timeline")
    public AnalyticsDto.MpDeviationsTimeline mpDeviationsTimeline(String slug, int months) {
        int m = Math.max(1, Math.min(24, months));
        LocalDate today = LocalDate.now(TALLINN);
        LocalDate from = today.minusMonths(m);
        int totalDev = 0, totalElig = 0;
        // Per-day deviation + eligible counts, bucketed by the vote's Tallinn calendar date.
        String sql2 = """
            SELECT
              (ve.started_at AT TIME ZONE 'Europe/Tallinn')::date AS d,
              SUM(CASE WHEN iv.choice <> a.majority_choice THEN 1 ELSE 0 END) AS dev,
              COUNT(*) AS elig
            FROM individual_vote iv
            JOIN vote_faction_alignment a
              ON a.vote_event_id = iv.vote_event_id
             AND a.faction_external_id = iv.faction_external_id
             AND a.has_clear_majority = TRUE
             AND a.majority_choice IN ('FOR','AGAINST','ABSTAINED')
            JOIN plenary_member pm ON pm.id = iv.plenary_member_id
            JOIN vote_event ve ON ve.id = iv.vote_event_id
            WHERE pm.slug = :slug
              AND iv.choice IN ('FOR','AGAINST','ABSTAINED')
              AND ve.started_at IS NOT NULL
              AND ve.started_at >= :fromTs
            GROUP BY 1
            ORDER BY 1
            """;
        List<Object[]> rows2 = em.createNativeQuery(sql2)
                .setParameter("slug", slug)
                .setParameter("fromTs", from.atStartOfDay(TALLINN).toInstant())
                .getResultList();
        List<AnalyticsDto.DeviationDayCell> days = new ArrayList<>();
        for (Object[] r : rows2) {
            LocalDate d = ((java.sql.Date) r[0]).toLocalDate();
            int dev = ((Number) r[1]).intValue();
            int elig = ((Number) r[2]).intValue();
            days.add(new AnalyticsDto.DeviationDayCell(d, dev, elig));
            totalDev += dev;
            totalElig += elig;
        }
        return new AnalyticsDto.MpDeviationsTimeline(slug, days, totalDev, totalElig, from, today);
    }

    /* ============================================================
     *  MP similar peers
     * ============================================================ */
    @Cacheable("analytics-mp-similar-peers")
    public AnalyticsDto.MpSimilarPeers mpSimilarPeers(String slug, int limit, int minOverlap) {
        int lim = Math.max(1, Math.min(20, limit));
        int minOv = Math.max(1, minOverlap);
        // For every other MP: count same/different comparable-choice pairs
        String sql = """
            SELECT
              b.plenary_member_id AS other_id,
              SUM(CASE WHEN a.choice = b.choice THEN 1 ELSE 0 END) AS same,
              SUM(CASE WHEN a.choice <> b.choice THEN 1 ELSE 0 END) AS diff,
              COUNT(*) AS total
            FROM individual_vote a
            JOIN plenary_member pm ON pm.id = a.plenary_member_id AND pm.slug = :slug
            JOIN individual_vote b ON b.vote_event_id = a.vote_event_id
              AND b.plenary_member_id <> a.plenary_member_id
            WHERE a.choice IN ('FOR','AGAINST','ABSTAINED')
              AND b.choice IN ('FOR','AGAINST','ABSTAINED')
            GROUP BY b.plenary_member_id
            HAVING COUNT(*) >= :minOv
            """;
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("slug", slug)
                .setParameter("minOv", (long) minOv)
                .getResultList();

        Map<String, Group> factionByExt = activeFactions().stream()
                .collect(Collectors.toMap(Group::getExternalId, g -> g, (a, b) -> a));
        Map<UUID, PlenaryMember> byId = memberRepo.findByActiveTrueOrderByLastNameAscFirstNameAsc().stream()
                .collect(Collectors.toMap(PlenaryMember::getId, m -> m, (a, b) -> a));

        record Row(PlenaryMember m, int same, int diff, int total, double rate) {}
        List<Row> ranked = new ArrayList<>();
        for (Object[] r : rows) {
            UUID id = uuidOf(r[0]);
            PlenaryMember m = byId.get(id);
            if (m == null) continue;
            long same = ((Number) r[1]).longValue();
            long diff = ((Number) r[2]).longValue();
            long total = ((Number) r[3]).longValue();
            if (total == 0) continue;
            ranked.add(new Row(m, (int) same, (int) diff, (int) total, (double) same / total));
        }
        List<AnalyticsDto.PeerAgreement> mostSimilar = ranked.stream()
                .sorted(Comparator.<Row>comparingDouble(r -> -r.rate).thenComparingInt(r -> -r.total))
                .limit(lim)
                .map(r -> toPeerDto(r.m, r.rate, r.total, r.same, r.diff, factionByExt))
                .toList();
        List<AnalyticsDto.PeerAgreement> mostOpposite = ranked.stream()
                .sorted(Comparator.<Row>comparingDouble(r -> r.rate).thenComparingInt(r -> -r.total))
                .limit(lim)
                .map(r -> toPeerDto(r.m, r.rate, r.total, r.same, r.diff, factionByExt))
                .toList();
        return new AnalyticsDto.MpSimilarPeers(slug, mostSimilar, mostOpposite, minOv, Instant.now());
    }
    private AnalyticsDto.PeerAgreement toPeerDto(PlenaryMember m, double rate, int total, int same, int diff,
                                                 Map<String, Group> factionByExt) {
        Group f = m.getFactionExternalId() != null ? factionByExt.get(m.getFactionExternalId()) : null;
        return new AnalyticsDto.PeerAgreement(
                m.getSlug(), m.getFullName(),
                f != null ? shortenFactionName(f.getName()) : shortenFactionName(m.getFactionName()),
                f != null ? f.getColorHex() : "#0072CE",
                rate, total, same, diff
        );
    }

    /* ============================================================
     *  Helpers
     * ============================================================ */
    private List<Group> activeFactions() {
        return groupRepo.findAll().stream()
                .filter(g -> g.getType() == GroupType.FRACTION && g.isActive())
                .sorted(Comparator.comparing(Group::getName))
                .toList();
    }

    private Map<String, Integer> seatsPerFaction() {
        String sql = """
            SELECT faction_external_id, COUNT(*) FROM plenary_member
            WHERE active = TRUE AND faction_external_id IS NOT NULL
            GROUP BY faction_external_id
            """;
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        Map<String, Integer> out = new HashMap<>();
        for (Object[] r : rows) {
            out.put((String) r[0], ((Number) r[1]).intValue());
        }
        return out;
    }

    /**
     * Which factions belong to the governing coalition vs opposition. Coalition membership is
     * config-driven (riigiluup.analytics.coalition-factions) so a change of government is a config
     * edit, not a code change; every faction not in that set counts as opposition.
     */
    private Map<String, String> coalitionRoleMap() {
        List<String> coalition = coalitionFactionNames.stream()
                .map(s -> s.trim().toLowerCase())
                .filter(s -> !s.isEmpty())
                .toList();
        Map<String, String> out = new HashMap<>();
        for (Group g : activeFactions()) {
            String n = g.getName().toLowerCase();
            boolean inCoalition = coalition.stream().anyMatch(n::contains);
            out.put(g.getExternalId(), inCoalition ? "coalition" : "opposition");
        }
        return out;
    }

    /** Short faction label: strip "fraktsioon" suffix + trailing spaces. */
    /**
     * Returns the recognisable party name (Estonian) used on axis labels and legends.
     * Prefers the party's proper name over " fraktsioon" or genitive forms;
     * only falls back to an acronym for the two parties known to the public
     * primarily by their acronyms (EKRE, SDE).
     */
    public static String shortenFactionName(String name) {
        if (name == null) return null;
        String s = name.replaceAll("(?i)\\s*fraktsioon\\s*$", "").trim();
        // Non-affiliated MPs group ("Fraktsiooni mittekuuluvad Riigikogu liikmed")
        if (s.toLowerCase().contains("mittekuuluv")) return "Sõltumatud";
        // Parties: proper name in nominative, no "Eesti " prefix (redundant on axis labels)
        if (s.contains("Reform")) return "Reformierakond";
        if (s.contains("Kesk")) return "Keskerakond";
        if (s.contains("Konservatiiv") || s.contains("EKRE")) return "EKRE";
        if (s.contains("Sotsiaal")) return "SDE";
        if (s.contains("Isamaa")) return "Isamaa";
        if (s.contains("Eesti 200") || s.contains("E200")) return "Eesti 200";
        return s;
    }

    private static double deterministicJitter(String seed, int salt) {
        // Simple LCG-like hash into [-1, 1]
        long h = (seed.hashCode() * 2654435761L) ^ (salt * 0x9E3779B97F4A7C15L);
        long m = (h & 0xffffffffL) % 10000;
        return (m / 10000.0) * 2.0 - 1.0;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Safe Instant converter — Hibernate 6 may return either java.time.Instant or java.sql.Timestamp. */
    private static Instant instantOf(Object o) {
        if (o == null) return null;
        if (o instanceof Instant i) return i;
        if (o instanceof java.sql.Timestamp t) return t.toInstant();
        if (o instanceof java.util.Date d) return d.toInstant();
        return null;
    }

    /** Safe UUID converter: JPA sometimes gives us bytes, sometimes UUID, sometimes String. */
    private static UUID uuidOf(Object o) {
        if (o == null) return null;
        if (o instanceof UUID u) return u;
        if (o instanceof String s) return UUID.fromString(s);
        if (o instanceof byte[] b && b.length == 16) {
            long msb = 0, lsb = 0;
            for (int i = 0; i < 8; i++) msb = (msb << 8) | (b[i] & 0xff);
            for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (b[i] & 0xff);
            return new UUID(msb, lsb);
        }
        return UUID.fromString(o.toString());
    }
}
