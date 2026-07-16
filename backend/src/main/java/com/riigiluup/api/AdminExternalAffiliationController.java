package com.riigiluup.api;

import com.riigiluup.party.ExternalAffiliation;
import com.riigiluup.party.ExternalAffiliationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CRUD for hand-curated MP external (non-Riigikogu) affiliations.
 * Protected under /api/v1/admin/** — auth handled by {@link com.riigiluup.admin.AdminSecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/admin/external-affiliations")
@RequiredArgsConstructor
public class AdminExternalAffiliationController {

    private final ExternalAffiliationRepository repo;

    /** All entries (optionally filtered by MP slug), newest-first for admin UI. */
    @GetMapping
    public List<AffiliationDto> list(@RequestParam(required = false) String slug) {
        List<ExternalAffiliation> rows = (slug == null || slug.isBlank())
                ? repo.findAll()
                : repo.findByMemberSlugOrderByValidFromAsc(slug);
        return rows.stream()
                .sorted((a, b) -> b.getValidFrom().compareTo(a.getValidFrom()))
                .map(AdminExternalAffiliationController::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AffiliationDto> one(@PathVariable UUID id) {
        return repo.findById(id).map(e -> ResponseEntity.ok(toDto(e))).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public AffiliationDto create(@RequestBody UpsertRequest req, Principal principal) {
        ExternalAffiliation e = new ExternalAffiliation();
        e.setCreatedAt(Instant.now());
        applyTo(e, req, principal);
        return toDto(repo.save(e));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AffiliationDto> update(@PathVariable UUID id, @RequestBody UpsertRequest req, Principal principal) {
        return repo.findById(id).map(e -> {
            applyTo(e, req, principal);
            return ResponseEntity.ok(toDto(repo.save(e)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Fill entity from request. verifiedBy defaults to auth principal if not set explicitly. */
    private static void applyTo(ExternalAffiliation e, UpsertRequest req, Principal principal) {
        if (req.memberSlug() == null || req.memberSlug().isBlank())
            throw new IllegalArgumentException("memberSlug is required");
        if (req.organization() == null || req.organization().isBlank())
            throw new IllegalArgumentException("organization is required");
        if (req.orgKind() == null || req.orgKind().isBlank())
            throw new IllegalArgumentException("orgKind is required");
        if (req.validFrom() == null)
            throw new IllegalArgumentException("validFrom is required");
        if (req.sourceUrl() == null || req.sourceUrl().isBlank())
            throw new IllegalArgumentException("sourceUrl is required");
        if (req.sourceLabel() == null || req.sourceLabel().isBlank())
            throw new IllegalArgumentException("sourceLabel is required");

        e.setMemberSlug(req.memberSlug().trim());
        e.setOrganization(req.organization().trim());
        e.setOrgKind(req.orgKind().trim());
        e.setRole(nullIfBlank(req.role()));
        e.setValidFrom(req.validFrom());
        e.setValidTo(req.validTo());
        e.setSourceUrl(req.sourceUrl().trim());
        e.setSourceLabel(req.sourceLabel().trim());
        e.setNote(nullIfBlank(req.note()));

        String verifier = nullIfBlank(req.verifiedBy());
        if (verifier == null) verifier = principal == null ? "admin" : principal.getName();
        e.setVerifiedBy(verifier);
        e.setVerifiedAt(req.verifiedAt() != null ? req.verifiedAt() : LocalDate.now());
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static AffiliationDto toDto(ExternalAffiliation e) {
        return new AffiliationDto(
                e.getId(), e.getMemberSlug(),
                e.getOrganization(), e.getOrgKind(), e.getRole(),
                e.getValidFrom(), e.getValidTo(),
                e.getSourceUrl(), e.getSourceLabel(),
                e.getVerifiedBy(), e.getVerifiedAt(),
                e.getNote(), e.getCreatedAt()
        );
    }

    public record AffiliationDto(
            UUID id, String memberSlug,
            String organization, String orgKind, String role,
            LocalDate validFrom, LocalDate validTo,
            String sourceUrl, String sourceLabel,
            String verifiedBy, LocalDate verifiedAt,
            String note, Instant createdAt
    ) {}

    public record UpsertRequest(
            String memberSlug, String organization, String orgKind, String role,
            LocalDate validFrom, LocalDate validTo,
            String sourceUrl, String sourceLabel,
            String verifiedBy, LocalDate verifiedAt,
            String note
    ) {}
}
