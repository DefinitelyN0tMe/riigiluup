package com.riigiluup.group;

import java.util.List;

/**
 * Read models for the parliamentary-groups directory (/groups): the friendship groups
 * (parlamendirühm), topic support groups (ühendus / toetusrühm) and international delegations an MP
 * can belong to. These exist on the Riigikogu site only as a flat per-MP tab; here they are also
 * browsable the other way round — group to its members. {@code category} is one of
 * FRIENDSHIP | SUPPORT | DELEGATION, derived from the group type.
 */
public final class GroupDirectoryDto {

    private GroupDirectoryDto() {
    }

    /** One row in the directory. externalId is the Riigikogu group UUID. */
    public record ListItem(String externalId, String name, String category, long memberCount) {
    }

    /** A member of a group. slug -> /politicians/{slug}; faction is denormalised on the member. */
    public record Member(String slug, String name, String factionName) {
    }

    public record Detail(String externalId, String name, String category, List<Member> members) {
    }

    /** Maps a group type to the public category label, or null if it is not a directory group. */
    public static String categoryOf(GroupType type) {
        return switch (type) {
            case BILATERAL_GROUP -> "FRIENDSHIP";
            case ASSOCIATION -> "SUPPORT";
            case DELEGATION -> "DELEGATION";
            default -> null;
        };
    }
}
