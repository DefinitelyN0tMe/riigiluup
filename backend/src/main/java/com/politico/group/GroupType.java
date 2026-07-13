package com.politico.group;

/**
 * Discriminator mapped from Riigikogu usergroups {@code type.code}.
 * Values observed so far: fraktsioon, alatine_komisjon, erikomisjon,
 * delegatsioon, uhendus (ühendus), parlamentaargrupp. Anything else → OTHER.
 */
public enum GroupType {
    FRACTION,
    STANDING_COMMITTEE,
    SPECIAL_COMMITTEE,
    DELEGATION,
    ASSOCIATION,
    BILATERAL_GROUP,
    OTHER;

    public static GroupType fromSourceCode(String code) {
        if (code == null) return OTHER;
        return switch (code.toLowerCase()) {
            case "fraktsioon" -> FRACTION;
            case "alatine_komisjon" -> STANDING_COMMITTEE;
            case "erikomisjon" -> SPECIAL_COMMITTEE;
            case "delegatsioon" -> DELEGATION;
            case "uhendus", "ühendus" -> ASSOCIATION;
            case "parlamentaargrupp" -> BILATERAL_GROUP;
            default -> OTHER;
        };
    }
}
