package com.riigiluup.api;

import com.riigiluup.common.PageResponse;
import com.riigiluup.vote.IndividualVoteRepository;
import com.riigiluup.vote.VoteEvent;
import com.riigiluup.vote.VoteEventRepository;
import com.riigiluup.vote.VoteEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VoteController {

    // Date-range params name Estonian calendar days; convert at the Tallinn boundary so
    // late-evening votes fall in the right day (stored instants are absolute/UTC).
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Tallinn");

    private final VoteEventRepository voteEventRepo;
    private final IndividualVoteRepository individualVoteRepo;
    private final VoteDetailMapper mapper;

    @GetMapping
    @Transactional(readOnly = true) // keep the session open so the mapper can read each row's bill title
    public PageResponse<VoteListItemDto> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer hour,
            @RequestParam(required = false) Integer dow,
            @RequestParam(required = false) Boolean onlyWeekend,
            @RequestParam(required = false) Boolean nightOnly,
            @RequestParam(required = false) Boolean lateOnly,
            @RequestParam(required = false) String factionA,
            @RequestParam(required = false) String factionB,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Instant fromTs = from == null ? null : from.atStartOfDay(DISPLAY_ZONE).toInstant();
        Instant toTs = to == null ? null : to.plusDays(1).atStartOfDay(DISPLAY_ZONE).toInstant();
        String typeParam = (type == null || type.isBlank()) ? null : type.toUpperCase();
        if (typeParam != null) VoteEventType.valueOf(typeParam);
        // Bounds-check hour and dow so an obvious client typo is a 400, not silent empty.
        if (hour != null && (hour < 0 || hour > 23))
            throw new IllegalArgumentException("hour must be 0..23");
        if (dow != null && (dow < 0 || dow > 6))
            throw new IllegalArgumentException("dow must be 0..6 (Mon..Sun)");
        int pageIdx = Math.min(Math.max(0, page), 10_000);
        int pageSize = Math.min(Math.max(size, 1), 100);

        boolean anyRich = hour != null || dow != null
                || Boolean.TRUE.equals(onlyWeekend)
                || Boolean.TRUE.equals(nightOnly)
                || Boolean.TRUE.equals(lateOnly)
                || (factionA != null && !factionA.isBlank() && factionB != null && !factionB.isBlank());
        Page<VoteEvent> p;
        if (anyRich) {
            p = voteEventRepo.richSearch(fromTs, toTs, typeParam,
                    hour, dow, onlyWeekend, nightOnly, lateOnly,
                    (factionA == null || factionA.isBlank()) ? null : factionA,
                    (factionB == null || factionB.isBlank()) ? null : factionB,
                    PageRequest.of(pageIdx, pageSize));
        } else {
            VoteEventType typeEnum = typeParam == null ? null : VoteEventType.valueOf(typeParam);
            p = voteEventRepo.search(fromTs, toTs, typeEnum,
                    PageRequest.of(pageIdx, pageSize));
        }
        return PageResponse.of(p.map(mapper::toListItem));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<VoteDetailDto> detail(@PathVariable UUID id) {
        VoteEvent v = voteEventRepo.findById(id).orElse(null);
        if (v == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(mapper.toDetail(v,
                individualVoteRepo.findByVoteEventWithMemberOrderByFactionNameAscPlenaryMember_LastNameAsc(v)));
    }
}
