package com.riigiluup.initiative;

import java.util.Arrays;
import java.util.Optional;

/**
 * The Riigikogu standing committee a citizen initiative was assigned to.
 *
 * <p>rahvaalgatus.ee emits a stable slug; we resolve it to our own {@code group} row by the
 * committee's Estonian name rather than by UUID. Reason: the DB holds 20 STANDING_COMMITTEE
 * rows, 9 of them inactive committees of earlier terms (Eelarve- ja maksukomisjon,
 * Riigiõiguskomisjon, …). A committee's UUID changes between terms; the institution's name
 * does not, and the source slug is term-agnostic — so the correct target is always the
 * currently active committee with this name.
 *
 * <p>Verified 2026-07-17: the 11 slugs map 1:1 onto the 11 active standing committees.
 */
public enum InitiativeCommittee {

    CONSTITUTIONAL("constitutional", "Põhiseaduskomisjon"),
    CULTURAL_AFFAIRS("cultural-affairs", "Kultuurikomisjon"),
    ECONOMIC_AFFAIRS("economic-affairs", "Majanduskomisjon"),
    ENVIRONMENT("environment", "Keskkonnakomisjon"),
    EU_AFFAIRS("eu-affairs", "Euroopa Liidu asjade komisjon"),
    FINANCE("finance", "Rahanduskomisjon"),
    FOREIGN_AFFAIRS("foreign-affairs", "Väliskomisjon"),
    LEGAL_AFFAIRS("legal-affairs", "Õiguskomisjon"),
    NATIONAL_DEFENCE("national-defence", "Riigikaitsekomisjon"),
    RURAL_AFFAIRS("rural-affairs", "Maaelukomisjon"),
    SOCIAL_AFFAIRS("social-affairs", "Sotsiaalkomisjon");

    private final String slug;
    private final String committeeName;

    InitiativeCommittee(String slug, String committeeName) {
        this.slug = slug;
        this.committeeName = committeeName;
    }

    public String slug() {
        return slug;
    }

    /** Matches {@code group.name} for the active STANDING_COMMITTEE row. */
    public String committeeName() {
        return committeeName;
    }

    /**
     * @return empty for blank or unrecognised input — a renamed or added committee must not
     *         fail the import; the raw slug is kept and the group link stays NULL.
     */
    public static Optional<InitiativeCommittee> fromSlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return Arrays.stream(values()).filter(c -> c.slug.equals(slug)).findFirst();
    }
}
