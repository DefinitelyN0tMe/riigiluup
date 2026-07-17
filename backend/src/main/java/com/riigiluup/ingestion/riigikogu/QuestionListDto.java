package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * HAL page of /api/volumes/interpellations and /api/volumes/written-questions.
 * NB: addressee.role is an OBJECT ({name: "välisminister X"}) on written questions but a
 * plain STRING ("peaminister X") on interpellations — hence JsonNode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuestionListDto(Embedded _embedded, Page page) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Embedded(List<Item> content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Page(int size, long totalElements, int totalPages, int number) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String uuid,
            Integer mark,
            Integer membership,
            String title,
            Addressee addressee,
            String submittingDate,
            String answerDeadline,
            AnswerDocument answerDocument,
            AgendaItem agendaItem
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Addressee(String uuid, String name, JsonNode role) {}

    /** Written-question reply document; respondDate only populated on the LIST endpoint. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AnswerDocument(String respondDate) {}

    /** Plenary agenda item where an interpellation was answered orally. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgendaItem(String uuid, String sittingDate) {}
}
