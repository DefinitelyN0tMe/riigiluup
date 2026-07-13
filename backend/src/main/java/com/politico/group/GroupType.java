package com.politico.group;

/**
 * Discriminator mapped from Riigikogu usergroups {@code type.code}.
 * Real codes observed in production API (uppercase; matcher lowercases them):
 * FRAKTSIOON, ALALINE_KOMISJON, ERIKOMISJON, UURIMISKOMISJON, DELEGATSIOON,
 * YHENDUS, PARLAMENDIRYHM, OSAKOND, ASUTUSE_YKSUS, RIIGIKOGU_JUHATUS,
 * RIIGIKOGU_TAISKOGU. Anything unrecognized falls through to OTHER.
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
            case "alaline_komisjon" -> STANDING_COMMITTEE;
            case "erikomisjon", "uurimiskomisjon" -> SPECIAL_COMMITTEE;
            case "delegatsioon" -> DELEGATION;
            case "yhendus", "uhendus", "ühendus" -> ASSOCIATION;
            case "parlamendiryhm", "parlamentaargrupp" -> BILATERAL_GROUP;
            default -> OTHER;
        };
    }
}
