package com.politico.vote;

public enum VoteChoice {
    FOR,
    AGAINST,
    ABSTAINED,
    DID_NOT_VOTE,
    ABSENT,
    PRESENT,
    UNKNOWN;

    public static VoteChoice fromSourceCode(String code) {
        if (code == null) return UNKNOWN;
        return switch (code.toUpperCase()) {
            case "POOLT" -> FOR;
            case "VASTU" -> AGAINST;
            case "ERAPOOLETU" -> ABSTAINED;
            case "EI_HAALETANUD" -> DID_NOT_VOTE;
            case "PUUDUB" -> ABSENT;
            case "KOHAL" -> PRESENT;
            default -> UNKNOWN;
        };
    }
}
