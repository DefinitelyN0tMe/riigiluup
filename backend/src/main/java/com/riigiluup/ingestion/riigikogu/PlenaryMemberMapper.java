package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.person.PlenaryMember;
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

    /**
     * Stub for a historical voter (former MP) discovered in a vote payload.
     *
     * <p>Riigikogu's {@code /api/plenary-members} endpoint only returns the CURRENT
     * mandate, so backfilling old votes surfaces UUIDs we've never seen. Rather than
     * skip those rows (silent data loss) or spend an extra API request per novel MP,
     * we materialise a minimal stub from what the vote payload itself carries.
     * The stub is marked {@code active=false} so frontend can label it "endine saadik".
     */
    public PlenaryMember stubFromVoter(String uuid, String fullName,
                                       String factionExternalId, String factionName) {
        Instant now = Instant.now();
        String safeFull = fullName == null || fullName.isBlank() ? uuid : fullName.trim();
        String[] parts = splitName(safeFull);
        return PlenaryMember.builder()
                .externalId(uuid)
                .sourceName("riigikogu")
                .firstName(parts[0])
                .lastName(parts[1])
                .fullName(safeFull)
                .slug(slugify(safeFull))
                .officialProfileUrl(
                        "https://www.riigikogu.ee/riigikogu-liikmed/liige/" + uuid + "/"
                )
                .active(false)
                .factionExternalId(factionExternalId)
                .factionName(factionName)
                .importedAt(now)
                .updatedAt(now)
                .build();
    }

    /** Split "Given Sur Name" → ["Given", "Sur Name"]. Compound surnames go to lastName. */
    static String[] splitName(String full) {
        int sp = full.indexOf(' ');
        if (sp < 0) return new String[]{full, ""};
        return new String[]{full.substring(0, sp), full.substring(sp + 1)};
    }

    static String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
