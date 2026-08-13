package com.riigiluup.api;

import com.riigiluup.alignment.PairwiseAgreementService;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.statistics.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/comparisons")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComparisonController {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Tallinn");

    private final PlenaryMemberRepository memberRepo;
    private final PairwiseAgreementService pairwiseService;
    private final ComparisonMapper mapper;
    private final com.riigiluup.group.GroupRepository groupRepo;
    private final com.riigiluup.alignment.VoteFactionAlignmentRepository alignmentRepo;

    @GetMapping("/politicians")
    @Transactional(readOnly = true)
    public ResponseEntity<ComparisonDto> compare(
            @RequestParam String leftSlug,
            @RequestParam String rightSlug,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        PlenaryMember left = memberRepo.findBySlug(leftSlug).orElse(null);
        PlenaryMember right = memberRepo.findBySlug(rightSlug).orElse(null);
        if (left == null || right == null) return ResponseEntity.notFound().build();

        LocalDate today = LocalDate.now(DISPLAY_ZONE);
        LocalDate fromDate = from != null ? from : StatisticsService.TERM_START;
        LocalDate toDate = to != null ? to : today;
        Instant fromTs = fromDate.atStartOfDay(DISPLAY_ZONE).toInstant();
        Instant toTs = toDate.plusDays(1).atStartOfDay(DISPLAY_ZONE).toInstant();

        PairwiseAgreementService.Result agreement =
                pairwiseService.forPair(left, right, fromTs, toTs);
        var disagreements = pairwiseService.recentDisagreements(left, right, 10);

        return ResponseEntity.ok(mapper.build(left, right, fromDate, toDate, agreement, disagreements));
    }

    /** Pairwise comparison of two factions by their voting majorities (party-level agreement). */
    @GetMapping("/factions")
    @Transactional(readOnly = true)
    public ResponseEntity<FactionComparisonDto> compareFactions(
            @RequestParam String left,
            @RequestParam String right
    ) {
        var lg = groupRepo.findFirstByExternalId(left).orElse(null);
        var rg = groupRepo.findFirstByExternalId(right).orElse(null);
        if (lg == null || rg == null || left.equals(right)) return ResponseEntity.notFound().build();

        java.util.List<Object[]> counts = alignmentRepo.pairwiseCounts(left, right);
        long same = counts.isEmpty() ? 0 : num(counts.get(0)[0]);
        long diff = counts.isEmpty() ? 0 : num(counts.get(0)[1]);
        int overlap = (int) (same + diff);
        Double rate = overlap == 0 ? null : (double) same / overlap;

        java.util.List<FactionComparisonDto.Disagreement> dis = alignmentRepo
                .recentDisagreements(left, right, 10).stream()
                .map(r -> new FactionComparisonDto.Disagreement(
                        r[0] == null ? null : r[0].toString(),
                        (String) r[1],
                        r[2] == null ? null : r[2].toString(),
                        (String) r[3],
                        (String) r[4]))
                .toList();

        FactionComparisonDto dto = new FactionComparisonDto(
                new FactionComparisonDto.Side(left, lg.getName(),
                        memberRepo.countByActiveTrueAndFactionExternalId(left)),
                new FactionComparisonDto.Side(right, rg.getName(),
                        memberRepo.countByActiveTrueAndFactionExternalId(right)),
                (int) same, (int) diff, overlap, rate, dis,
                "Party agreement = (matching faction-majority) / (votes where both factions had a clear majority), current term.");
        return ResponseEntity.ok(dto);
    }

    private static long num(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }
}
