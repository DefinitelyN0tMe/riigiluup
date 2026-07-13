package com.politico.api;

import com.politico.common.PageResponse;
import com.politico.legislation.LegislationPhase;
import com.politico.legislation.LegislativeItem;
import com.politico.legislation.LegislativeItemRepository;
import com.politico.legislation.LegislativeItemTopicRepository;
import com.politico.legislation.LegislativeSponsorshipRepository;
import com.politico.legislation.LegislativeStageRepository;
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
    private final LegislationMapper mapper;

    @GetMapping
    public PageResponse<LegislationListItemDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) Integer membership,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        LegislationPhase phaseEnum = (phase == null || phase.isBlank())
                ? null : LegislationPhase.valueOf(phase);
        Page<LegislativeItem> p = itemRepo.search(
                q, phaseEnum, membership,
                PageRequest.of(page, Math.min(size, 100)));
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
                itemTopicRepo.findByLegislativeItem(item)));
    }
}
