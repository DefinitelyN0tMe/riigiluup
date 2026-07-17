package com.riigiluup.initiative;

import java.util.Arrays;

/**
 * Lifecycle phase as rahvaalgatus.ee reports it. The slug is what the source emits and what
 * the DB column stores — the CHECK constraint in V26 lists exactly these five values.
 */
public enum InitiativePhase {

    EDIT("edit"),
    SIGN("sign"),
    PARLIAMENT("parliament"),
    GOVERNMENT("government"),
    DONE("done");

    private final String slug;

    InitiativePhase(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    /**
     * @return null for blank input; throws for an unrecognised slug — a new source value must
     *         fail the import run loudly (visible in /admin) rather than be silently dropped.
     */
    public static InitiativePhase fromSlug(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return Arrays.stream(values())
                .filter(p -> p.slug.equals(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown rahvaalgatus phase: '" + slug + "'"));
    }
}
