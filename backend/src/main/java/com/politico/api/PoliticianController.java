package com.politico.api;

import com.politico.common.PageResponse;
import com.politico.person.PlenaryMember;
import com.politico.person.PlenaryMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Page<PlenaryMember> p = repo.search(
                q, activeOnly,
                PageRequest.of(page, Math.min(size, 100),
                        Sort.by("lastName", "firstName"))
        );
        return PageResponse.of(p.map(mapper::toDto));
    }
}
