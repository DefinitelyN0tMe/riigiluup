package com.riigiluup.initiative;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Public initiative API. Every endpoint is implicitly restricted to initiatives addressed to
 * Riigikogu — municipal ones are stored but never served.
 */
@RestController
@RequestMapping("/api/v1/initiatives")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InitiativeController {

    private final InitiativeService service;

    @GetMapping
    public InitiativeDto.ListPage list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) String committee,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Clamp both bounds like the other list controllers: a negative page/size becomes a
        // negative SQL OFFSET/LIMIT and a raw 500 instead of a clean, cacheable response.
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        return service.list(q, phase, decision, committee, safePage, safeSize);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InitiativeDto.Detail> detail(@PathVariable Long id) {
        return service.detail(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-legislative-item/{legislativeItemId}")
    public List<InitiativeDto.ListItem> byLegislativeItem(@PathVariable UUID legislativeItemId) {
        return service.byLegislativeItem(legislativeItemId);
    }
}
