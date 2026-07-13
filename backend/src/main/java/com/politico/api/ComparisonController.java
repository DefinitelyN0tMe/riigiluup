package com.politico.api;

import com.politico.alignment.PairwiseAgreementService;
import com.politico.person.PlenaryMember;
import com.politico.person.PlenaryMemberRepository;
import com.politico.statistics.StatisticsService;
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
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/comparisons")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComparisonController {

    private final PlenaryMemberRepository memberRepo;
    private final PairwiseAgreementService pairwiseService;
    private final ComparisonMapper mapper;

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

        LocalDate today = LocalDate.now();
        LocalDate fromDate = from != null ? from : StatisticsService.TERM_START;
        LocalDate toDate = to != null ? to : today;
        Instant fromTs = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toTs = toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        PairwiseAgreementService.Result agreement =
                pairwiseService.forPair(left, right, fromTs, toTs);
        var disagreements = pairwiseService.recentDisagreements(left, right, 10);

        return ResponseEntity.ok(mapper.build(left, right, fromDate, toDate, agreement, disagreements));
    }
}
