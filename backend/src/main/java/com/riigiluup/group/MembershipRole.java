package com.riigiluup.group;

/**
 * Role of an MP within a group. Riigikogu detail responses give Estonian labels
 * such as {@code esimees} (chair), {@code aseesimees} (vice-chair), plain member is blank.
 */
public enum MembershipRole {
    MEMBER,
    CHAIR,
    VICE_CHAIR,
    REPRESENTATIVE,
    OTHER;

    public static MembershipRole fromSourceLabel(String label) {
        if (label == null || label.isBlank()) return MEMBER;
        return switch (label.toLowerCase()) {
            case "esimees", "chair" -> CHAIR;
            case "aseesimees", "vice-chair" -> VICE_CHAIR;
            case "esindaja", "representative" -> REPRESENTATIVE;
            case "liige", "member" -> MEMBER;
            default -> OTHER;
        };
    }
}
