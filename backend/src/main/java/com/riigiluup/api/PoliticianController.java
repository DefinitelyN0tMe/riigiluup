package com.riigiluup.api;

import com.riigiluup.common.PageResponse;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/politicians")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PoliticianController {

    private final PlenaryMemberRepository repo;
    private final PoliticianMapper mapper;

    @GetMapping
    public PageResponse<PoliticianDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String faction,
            // current = sitting MPs (default, the site's primary view); former = left the
            // composition (ministers / resigned); all = both.
            @RequestParam(defaultValue = "current") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Boolean active = switch (status) {
            case "former" -> Boolean.FALSE;
            case "all" -> null;
            default -> Boolean.TRUE; // "current"
        };
        Page<PlenaryMember> p = repo.searchByFaction(
                q, faction, active,
                PageRequest.of(Math.min(Math.max(0, page), 10_000), Math.min(size, 100),
                        Sort.by("lastName", "firstName"))
        );
        return PageResponse.of(p.map(mapper::toDto));
    }

    @GetMapping("/factions")
    public List<FactionOption> factions() {
        return repo.findDistinctFactionsForActiveMembers();
    }

    public record FactionOption(String externalId, String name, long memberCount) {}
}
