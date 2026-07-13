package com.politico.ingestion.riigikogu;

import com.politico.person.PlenaryMember;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
public class PlenaryMemberDetailMapper {

    private static final String BASE = "https://api.riigikogu.ee";

    public void applyDetail(PlenaryMember target, PlenaryMemberDetailDto dto) {
        target.setEmail(dto.email());
        target.setGender(dto.gender());
        target.setDateOfBirth(parseDate(dto.dateOfBirth()));
        target.setBiographyHtml(dto.biography());
        target.setParliamentSeniorityDays(dto.parliamentSeniority());
        target.setPhotoUrl(photoDownloadUrl(dto));

        PlenaryMemberDetailDto.Membership current = currentTerm(dto);
        target.setElectoralDistrict(firstDistrictName(current));
        applyCurrentFaction(target, current);
        target.setUpdatedAt(Instant.now());
    }

    /** Current-term (active) committee refs, or empty list if MP is not currently serving. */
    public List<PlenaryMemberDetailDto.GroupRef> currentTermCommittees(PlenaryMemberDetailDto dto) {
        PlenaryMemberDetailDto.Membership term = currentTerm(dto);
        if (term == null || term.committees() == null) return List.of();
        return term.committees().stream()
                .filter(g -> g.membership() == null || g.membership().endDate() == null)
                .toList();
    }

    static PlenaryMemberDetailDto.Membership currentTerm(PlenaryMemberDetailDto dto) {
        if (dto.memberships() == null || dto.memberships().isEmpty()) return null;
        return dto.memberships().stream()
                .filter(PlenaryMemberDetailMapper::hasActiveRoleItem)
                .max(Comparator.comparing(
                        m -> m.membershipNumber() == null ? 0 : m.membershipNumber()))
                .orElse(null);
    }

    private static boolean hasActiveRoleItem(PlenaryMemberDetailDto.Membership m) {
        if (m.membershipRoleItems() == null) return false;
        return m.membershipRoleItems().stream().anyMatch(r -> r.endDate() == null);
    }

    private static LocalDate parseDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return LocalDate.parse(iso); }
        catch (Exception e) { return null; }
    }

    private static String firstDistrictName(PlenaryMemberDetailDto.Membership term) {
        if (term == null || term.electoralDistrict() == null || term.electoralDistrict().isEmpty()) {
            return null;
        }
        var entry = term.electoralDistrict().get(0);
        return entry.electoralDistrict() == null ? null : entry.electoralDistrict().value();
    }

    private static void applyCurrentFaction(
            PlenaryMember target, PlenaryMemberDetailDto.Membership term
    ) {
        if (term == null || term.factions() == null || term.factions().isEmpty()) return;
        term.factions().stream()
                .filter(f -> f.membership() == null || f.membership().endDate() == null)
                .findFirst()
                .ifPresent(f -> {
                    target.setFactionExternalId(f.uuid());
                    target.setFactionName(f.name());
                });
    }

    private static String photoDownloadUrl(PlenaryMemberDetailDto dto) {
        if (dto.photo() == null
                || dto.photo()._links() == null
                || dto.photo()._links().download() == null) return null;
        String href = dto.photo()._links().download().href();
        if (href == null || href.isBlank()) return null;
        return href.startsWith("http") ? href : BASE + href;
    }
}
