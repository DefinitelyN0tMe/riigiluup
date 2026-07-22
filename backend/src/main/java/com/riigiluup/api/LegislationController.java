package com.riigiluup.api;

import com.riigiluup.common.PageResponse;
import com.riigiluup.legislation.LegislationPhase;
import com.riigiluup.legislation.LegislativeItem;
import com.riigiluup.legislation.LegislativeItemRepository;
import com.riigiluup.legislation.LegislativeItemTopicRepository;
import com.riigiluup.legislation.LegislativeSponsorshipRepository;
import com.riigiluup.legislation.LegislativeStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/legislation")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LegislationController {

    private final LegislativeItemRepository itemRepo;
    private final LegislativeStageRepository stageRepo;
    private final LegislativeSponsorshipRepository sponsorshipRepo;
    private final LegislativeItemTopicRepository itemTopicRepo;
    private final com.riigiluup.vote.VoteEventRepository voteEventRepo;
    private final LegislationMapper mapper;

    @GetMapping
    public PageResponse<LegislationListItemDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) Integer membership,
            @RequestParam(required = false) Integer topicEdid,
            @RequestParam(required = false) Integer minDays,
            @RequestParam(required = false) Integer maxDays,
            @RequestParam(required = false) String committee,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        String phaseParam = (phase == null || phase.isBlank()) ? null : phase.toUpperCase();
        // Validate enum name so we fail fast on typos rather than passing garbage to SQL
        if (phaseParam != null) LegislationPhase.valueOf(phaseParam);
        Page<LegislativeItem> p = itemRepo.search(
                (q == null || q.isBlank()) ? null : q,
                phaseParam, membership, topicEdid, minDays, maxDays,
                (committee == null || committee.isBlank()) ? null : committee,
                PageRequest.of(Math.min(Math.max(0, page), 10_000), Math.min(size, 100)));
        return PageResponse.of(p.map(mapper::toListItem));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<LegislationDetailDto> detail(@PathVariable UUID id) {
        LegislativeItem item = itemRepo.findById(id).orElse(null);
        if (item == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(mapper.toDetail(
                item,
                stageRepo.findByLegislativeItemOrderBySequenceAsc(item),
                sponsorshipRepo.findByLegislativeItem(item),
                itemTopicRepo.findByLegislativeItem(item),
                voteEventRepo.findByLegislativeItemOrderByStartedAtAsc(item)));
    }
}
