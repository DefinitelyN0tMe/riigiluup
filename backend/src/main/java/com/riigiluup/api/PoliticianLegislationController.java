package com.riigiluup.api;

import com.riigiluup.common.PageResponse;
import com.riigiluup.legislation.LegislativeItem;
import com.riigiluup.legislation.LegislativeSponsorshipRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
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

@RestController
@RequestMapping("/api/v1/politicians/{slug}/legislation")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PoliticianLegislationController {

    private final PlenaryMemberRepository memberRepo;
    private final LegislativeSponsorshipRepository sponsorshipRepo;
    private final LegislationMapper mapper;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Response> list(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PlenaryMember m = memberRepo.findBySlug(slug).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();
        long count = sponsorshipRepo.countByPlenaryMember(m);
        Page<LegislativeItem> p = sponsorshipRepo.findItemsSponsoredByMember(
                m, PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(new Response(
                (int) count,
                PageResponse.of(p.map(mapper::toListItem))
        ));
    }

    public record Response(int totalSponsored, PageResponse<LegislationListItemDto> items) {}
}
