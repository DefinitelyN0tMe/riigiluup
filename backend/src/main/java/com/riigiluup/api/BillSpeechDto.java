package com.riigiluup.api;

import com.riigiluup.speech.SpeechRepository;

import java.time.Instant;

/**
 * One speech tied to a bill, for the legislation detail page's debate section. The speaker is
 * linked to an MP profile when resolved (slug/name), else only the raw verbatim label is shown.
 * The excerpt is the speech opening; the full stenogram lives behind sourceUrl.
 */
public record BillSpeechDto(
        Long id,
        String speaker,
        String memberSlug,
        String memberName,
        Instant spokenAt,
        String sittingTitle,
        String agendaItemTitle,
        String sourceUrl,
        String excerpt
) {
    static BillSpeechDto from(SpeechRepository.SpeechSearchRow r) {
        return new BillSpeechDto(
                r.getId(),
                r.getSpeakerRaw(),
                r.getMemberSlug(),
                r.getMemberName(),
                r.getSpokenAt(),
                r.getSittingTitle(),
                r.getAgendaItemTitle(),
                r.getSourceUrl(),
                r.getExcerpt());
    }
}
