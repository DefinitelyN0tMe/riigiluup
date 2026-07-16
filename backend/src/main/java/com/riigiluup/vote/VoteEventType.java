package com.riigiluup.vote;

public enum VoteEventType {
    OPEN,
    ATTENDANCE_CHECK,
    SECRET,
    OTHER;

    public static VoteEventType fromSourceCode(String code) {
        if (code == null) return OTHER;
        return switch (code.toUpperCase()) {
            case "AVALIK" -> OPEN;
            case "KOHALOLEKU_KONTROLL" -> ATTENDANCE_CHECK;
            case "SALAJANE" -> SECRET;
            default -> OTHER;
        };
    }
}
