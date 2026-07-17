package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riigiluup.question.QuestionKind;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GovernmentQuestionMapperTest {

    private final ObjectMapper json = new ObjectMapper();

    private static QuestionListDto.Item item(QuestionListDto.AnswerDocument doc,
                                             QuestionListDto.AgendaItem agenda) {
        return new QuestionListDto.Item("u-1", 1, 15, "t", null,
                "2026-01-10", "2026-01-24", doc, agenda);
    }

    @Test
    void role_name_handles_object_string_and_null() throws Exception {
        // written questions: role is an object
        assertThat(GovernmentQuestionMapper.roleName(
                json.readTree("{\"name\":\"välisminister Margus Tsahkna\"}")))
                .isEqualTo("välisminister Margus Tsahkna");
        // interpellations: role is a plain string
        assertThat(GovernmentQuestionMapper.roleName(
                json.readTree("\"peaminister Kristen Michal\"")))
                .isEqualTo("peaminister Kristen Michal");
        assertThat(GovernmentQuestionMapper.roleName(null)).isNull();
        assertThat(GovernmentQuestionMapper.roleName(json.readTree("null"))).isNull();
    }

    @Test
    void person_name_is_normalized_when_source_mixes_role_into_the_name() {
        // written questions: clean split already
        assertThat(GovernmentQuestionMapper.personName("Kristen Michal", "peaminister Kristen Michal"))
                .isEqualTo("Kristen Michal");
        // interpellations sometimes put the role string INTO the name with role null
        assertThat(GovernmentQuestionMapper.personName("kultuuriminister Heidy Purga", null))
                .isEqualTo("Heidy Purga");
        assertThat(GovernmentQuestionMapper.personName(
                "energeetika- ja keskkonnaminister Andres Sutt", null))
                .isEqualTo("Andres Sutt");
        // plain name with no role stays as is
        assertThat(GovernmentQuestionMapper.personName("Kaja Kallas", null)).isEqualTo("Kaja Kallas");
        assertThat(GovernmentQuestionMapper.personName(null, null)).isNull();
    }

    @Test
    void role_falls_back_to_the_role_prefixed_name() {
        assertThat(GovernmentQuestionMapper.roleOrName("välisminister Margus Tsahkna", "Margus Tsahkna"))
                .isEqualTo("välisminister Margus Tsahkna");
        assertThat(GovernmentQuestionMapper.roleOrName(null, "kultuuriminister Heidy Purga"))
                .isEqualTo("kultuuriminister Heidy Purga");
        assertThat(GovernmentQuestionMapper.roleOrName(null, "Kaja Kallas")).isNull();
    }

    @Test
    void answered_date_comes_from_respond_date_for_written_questions() {
        QuestionListDto.Item answered = item(
                new QuestionListDto.AnswerDocument("2024-12-27"), null);
        QuestionListDto.Item open = item(null, null);

        assertThat(GovernmentQuestionMapper.answeredDate(QuestionKind.WRITTEN_QUESTION, answered))
                .isEqualTo(LocalDate.of(2024, 12, 27));
        assertThat(GovernmentQuestionMapper.answeredDate(QuestionKind.WRITTEN_QUESTION, open))
                .isNull();
    }

    @Test
    void answered_date_comes_from_sitting_date_for_interpellations() {
        QuestionListDto.Item answered = item(null,
                new QuestionListDto.AgendaItem("a-1", "2026-03-23"));
        QuestionListDto.Item open = item(null, null);

        assertThat(GovernmentQuestionMapper.answeredDate(QuestionKind.INTERPELLATION, answered))
                .isEqualTo(LocalDate.of(2026, 3, 23));
        assertThat(GovernmentQuestionMapper.answeredDate(QuestionKind.INTERPELLATION, open))
                .isNull();
    }
}
