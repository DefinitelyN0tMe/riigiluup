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
        return service.list(q, phase, decision, committee, page, Math.min(size, 100));
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
