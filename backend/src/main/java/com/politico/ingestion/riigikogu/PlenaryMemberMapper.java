package com.politico.ingestion.riigikogu;

import com.politico.person.PlenaryMember;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Instant;

@Component
public class PlenaryMemberMapper {

    public PlenaryMember toEntity(PlenaryMemberDto dto) {
        String fullName = dto.fullName() != null && !dto.fullName().isBlank()
                ? dto.fullName()
                : (dto.firstName() + " " + dto.lastName()).trim();

        Instant now = Instant.now();

        return PlenaryMember.builder()
                .externalId(dto.uuid())
                .sourceName("riigikogu")
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .fullName(fullName)
                .slug(slugify(fullName))
                .photoUrl(dto.photoUrl())
                .officialProfileUrl(
                        "https://www.riigikogu.ee/riigikogu-liikmed/liige/" + dto.uuid() + "/"
                )
                .active(Boolean.TRUE.equals(dto.active()))
                .factionExternalId(dto.faction() == null ? null : dto.faction().uuid())
                .factionName(dto.faction() == null ? null : dto.faction().name())
                .importedAt(now)
                .updatedAt(now)
                .build();
    }

    static String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
