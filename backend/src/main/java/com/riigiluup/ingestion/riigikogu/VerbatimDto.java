package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** One sitting's verbatim record from /api/steno/verbatims — agenda items with speech events. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VerbatimDto(
        Integer membership,
        Integer plenarySession,
        String link,
        String date,
        String title,
        Boolean edited,
        List<AgendaItem> agendaItems
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgendaItem(
            String agendaItemUuid,
            String date,
            String title,
            List<Event> events
    ) {}

    /** type is SPEECH | VOTING_EVENT | PRESENCE_CHECK | SESSION_END; text only set for SPEECH. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Event(
            String type,
            String uuid,
            String date,
            String speaker,
            String text,
            String link
    ) {}
}
