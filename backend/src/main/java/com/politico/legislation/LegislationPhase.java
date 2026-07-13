package com.politico.legislation;

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
            case "INITIATION" -> SUBMITTED;
            case "ESIMENE_LUGEMINE", "TEINE_LUGEMINE", "KOLMAS_LUGEMINE" -> IN_READINGS;
            case "KOMISJONI_MENETLUS", "KOMISJON" -> IN_COMMITTEE;
            case "VASTU_VOETUD" -> ADOPTED;
            case "TAGASI_LUKATUD" -> REJECTED;
            case "LOPETATUD", "TAGASI_VOETUD" -> WITHDRAWN;
            default -> OTHER;
        };
    }
}
