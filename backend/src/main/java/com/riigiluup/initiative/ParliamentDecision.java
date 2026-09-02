package com.riigiluup.initiative;

import java.util.Arrays;

/**
 * What Riigikogu decided about an initiative it accepted for proceedings. Slugs are the
 * source's own vocabulary and match the V26 CHECK constraint.
 */
public enum ParliamentDecision {

    RETURN("return"),
    REJECT("reject"),
    SOLVE_DIFFERENTLY("solve-differently"),
    FORWARD("forward"),
    FORWARD_TO_GOVERNMENT("forward-to-government"),
    DRAFT_ACT_OR_NATIONAL_MATTER("draft-act-or-national-matter");

    private final String slug;

    ParliamentDecision(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    /** @see InitiativePhase#fromSlug(String) for the null/throw contract. */
    public static ParliamentDecision fromSlug(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return Arrays.stream(values())
                .filter(d -> d.slug.equals(slug))
                .findFirst()
                .orElseThrow(() -> new UnknownSourceSlugException(
                        "Unknown rahvaalgatus parliament_decision: '" + slug + "'"));
    }
}
