package com.riigiluup.legislation;

/**
 * Coarse phases derived from Riigikogu {@code activeDraftStage}. The raw
 * source string is preserved separately in {@code active_stage_source_code}
 * so nothing is lost if Riigikogu adds a new stage.
 */
public enum LegislationPhase {
    SUBMITTED,
    IN_COMMITTEE,
    IN_READINGS,
    ADOPTED,
    REJECTED,
    WITHDRAWN,
    OTHER;

    public static LegislationPhase fromStageCode(String code) {
        if (code == null) return OTHER;
        return switch (code.toUpperCase()) {
            case "INITIATION", "ALGATATUD", "MENETLUSSE_VOETUD" -> SUBMITTED;
            case "ESIMENE_LUGEMINE", "TEINE_LUGEMINE", "KOLMAS_LUGEMINE" -> IN_READINGS;
            // Re-deliberation after the President declined to promulgate: the bill is back in the chamber.
            case "UUESTI_ARUTAMINE" -> IN_READINGS;
            case "KOMISJONI_MENETLUS", "KOMISJON" -> IN_COMMITTEE;
            case "VASTU_VOETUD" -> ADOPTED;
            // Source spells it TAGASI_LYKATUD (Y, not U); the old "TAGASI_LUKATUD" never matched, so
            // ~1500 rejected bills fell through to OTHER and the "rejected" filter was empty.
            case "TAGASI_LYKATUD" -> REJECTED;
            case "LOPETATUD", "TAGASI_VOETUD", "TAGASI_VOETUD_TAISKOGUL_MENETLEMATA", "TAGASTATUD" -> WITHDRAWN;
            // VALJA_LANGENUD*, YHENDATUD, VALJA_ARVATUD, VALJA_KUULUTAMATA_JAETUD etc. are genuinely other
            // terminal outcomes (lapsed / merged / excluded) that no coarse phase fits -> OTHER.
            default -> OTHER;
        };
    }
}
