package com.politico.ingestion.riigikogu;

import com.politico.person.PlenaryMember;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PlenaryMemberDetailMapper {

    private static final String BASE = "https://api.riigikogu.ee";

    public void applyDetail(PlenaryMember target, PlenaryMemberDetailDto dto) {
        target.setEmail(dto.email());
        target.setGender(dto.gender());
        target.setDateOfBirth(parseDate(dto.dateOfBirth()));
        target.setBiographyHtml(dto.biography());
        target.setParliamentSeniorityDays(dto.parliamentSeniority());
        target.setElectoralDistrict(firstDistrictName(dto));
        target.setPhotoUrl(photoDownloadUrl(dto));
        if (dto.currentFaction() != null) {
            target.setFactionExternalId(dto.currentFaction().uuid());
            target.setFactionName(dto.currentFaction().name());
        }
        target.setUpdatedAt(java.time.Instant.now());
    }

    private static LocalDate parseDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return LocalDate.parse(iso); }
        catch (Exception e) { return null; }
    }

    private static String firstDistrictName(PlenaryMemberDetailDto dto) {
        if (dto.electoralDistrict() == null || dto.electoralDistrict().isEmpty()) return null;
        return dto.electoralDistrict().get(0).name();
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
