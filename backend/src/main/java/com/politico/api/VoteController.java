package com.politico.api;

import com.politico.common.PageResponse;
import com.politico.vote.IndividualVoteRepository;
import com.politico.vote.VoteEvent;
import com.politico.vote.VoteEventRepository;
import com.politico.vote.VoteEventType;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VoteController {

    private final VoteEventRepository voteEventRepo;
    private final IndividualVoteRepository individualVoteRepo;
    private final VoteDetailMapper mapper;

    @GetMapping
    public PageResponse<VoteListItemDto> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Instant fromTs = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toTs = to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        VoteEventType typeEnum = type == null || type.isBlank() ? null : VoteEventType.valueOf(type);
        Page<VoteEvent> p = voteEventRepo.search(fromTs, toTs, typeEnum,
                PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(p.map(mapper::toListItem));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VoteDetailDto> detail(@PathVariable UUID id) {
        VoteEvent v = voteEventRepo.findById(id).orElse(null);
        if (v == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(mapper.toDetail(v,
                individualVoteRepo.findByVoteEventWithMemberOrderByFactionNameAscPlenaryMember_LastNameAsc(v)));
    }
}
