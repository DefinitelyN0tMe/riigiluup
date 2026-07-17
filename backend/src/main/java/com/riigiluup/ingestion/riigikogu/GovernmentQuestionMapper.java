package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.databind.JsonNode;
import com.riigiluup.question.QuestionKind;

import java.time.LocalDate;

/** Field extraction for interpellation / written-question volumes. */
public final class GovernmentQuestionMapper {

    private GovernmentQuestionMapper() {}

    /** addressee.role is an object on written questions but a plain string on interpellations. */
    public static String roleName(JsonNode role) {
        if (role == null || role.isNull()) return null;
        if (role.isTextual()) return role.asText();
        JsonNode name = role.path("name");
        return name.isTextual() ? name.asText() : null;
    }

    /**
     * The answer date the source itself records: a written reply document for written
     * questions, the plenary sitting where the minister answered for interpellations.
     */
    public static LocalDate answeredDate(QuestionKind kind, QuestionListDto.Item item) {
        if (kind == QuestionKind.WRITTEN_QUESTION) {
            return item.answerDocument() == null ? null : parseDate(item.answerDocument().respondDate());
        }
        return item.agendaItem() == null ? null : parseDate(item.agendaItem().sittingDate());
    }

    private static LocalDate parseDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        // respondDate occasionally arrives with a time part — keep the date only.
        return LocalDate.parse(iso.length() > 10 ? iso.substring(0, 10) : iso);
    }

    /**
     * The source is inconsistent: written questions carry a plain person name plus a role
     * object, while some interpellations put the whole role string INTO the name
     * ("kultuuriminister Heidy Purga") with role null — which would split one minister
     * into two analytics rows. Estonian role words are lowercase and person names are
     * capitalized, so the person is the trailing run of capitalized tokens.
     */
    public static String personName(String name, String roleName) {
        if (name == null || name.isBlank()) return null;
        String[] tokens = name.trim().split("\\s+");
        int start = tokens.length;
        while (start > 0 && Character.isUpperCase(tokens[start - 1].codePointAt(0))) {
            start--;
        }
        if (start == 0 || start == tokens.length) return name.trim();
        return String.join(" ", java.util.Arrays.copyOfRange(tokens, start, tokens.length));
    }

    /** Prefer the explicit role; fall back to a role-prefixed name (lowercase first token). */
    public static String roleOrName(String roleName, String name) {
        if (roleName != null && !roleName.isBlank()) return roleName;
        if (name == null || name.isBlank()) return null;
        String trimmed = name.trim();
        return Character.isLowerCase(trimmed.codePointAt(0)) ? trimmed : null;
    }
}
