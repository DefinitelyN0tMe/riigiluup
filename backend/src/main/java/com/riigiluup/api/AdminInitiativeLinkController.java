package com.riigiluup.api;

import com.riigiluup.initiative.Initiative;
import com.riigiluup.initiative.InitiativeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * Curated initiative → bill link. The source carries no bill reference, and title matching
 * would be guesswork (initiative titles are appeals, bill titles are legal names), so an
 * admin asserts the link against the primary source and we record who and when.
 */
@RestController
@RequestMapping("/api/v1/admin/initiatives")
@RequiredArgsConstructor
public class AdminInitiativeLinkController {

    private final InitiativeRepository repo;

    public record LinkRequest(UUID legislativeItemId) {
    }

    @PutMapping("/{id}/legislative-item")
    @Transactional
    public ResponseEntity<Void> link(
            @PathVariable Long id, @RequestBody LinkRequest body, Principal principal) {
        return repo.findById(id).map(i -> {
            i.setLegislativeItemId(body.legislativeItemId());
            i.setLinkedBy(principal == null ? "unknown" : principal.getName());
            i.setLinkedAt(Instant.now());
            repo.save(i);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/legislative-item")
    @Transactional
    public ResponseEntity<Void> unlink(@PathVariable Long id) {
        return repo.findById(id).map(i -> {
            i.setLegislativeItemId(null);
            i.setLinkedBy(null);
            i.setLinkedAt(null);
            repo.save(i);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
