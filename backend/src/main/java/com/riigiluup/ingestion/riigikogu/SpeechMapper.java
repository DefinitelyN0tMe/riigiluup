package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.person.PlenaryMember;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Flattens verbatim records into speech rows and resolves speakers against the MP roster. */
@Component
public class SpeechMapper {

    public record FlatSpeech(
            /** Person uuid of the speaker (the source's event `uuid` — misdocumented upstream). */
            String speakerUuid,
            String speakerRaw,
            Instant spokenAt,
            String sittingTitle,
            String sittingLink,
            String agendaItemTitle,
            String text,
            /** Riigikogu composition from the verbatim record; may be null on old sittings. */
            Integer membership
    ) {}

    public List<FlatSpeech> flatten(VerbatimDto verbatim) {
        List<FlatSpeech> out = new ArrayList<>();
        if (verbatim == null || verbatim.agendaItems() == null) return out;
        for (VerbatimDto.AgendaItem item : verbatim.agendaItems()) {
            if (item.events() == null) continue;
            for (VerbatimDto.Event e : item.events()) {
                if (!"SPEECH".equals(e.type())) continue;
                if (e.text() == null || e.text().isBlank()) continue;
                out.add(new FlatSpeech(
                        e.uuid(),
                        e.speaker() == null ? "—" : e.speaker(),
                        parseInstant(e.date()),
                        verbatim.title(),
                        verbatim.link(),
                        plainText(item.title()),
                        e.text(),
                        verbatim.membership()
                ));
            }
        }
        return out;
    }

    /** Verbatim timestamps carry an explicit offset (unlike votings), so no zone assumption here. */
    private static Instant parseInstant(String s) {
        return s == null ? null : OffsetDateTime.parse(s).toInstant();
    }

    /** Agenda item titles arrive as HTML fragments ("<p>…</p>") — keep only the text. */
    private static String plainText(String html) {
        return html == null ? null : Jsoup.parse(html).text();
    }

    /**
     * Resolves a verbatim speaker string ("Kaja Kallas", "Esimees Lauri Hussar",
     * "Peaminister Kristen Michal") against the MP roster. Role prefixes are handled by
     * progressively dropping leading tokens until the remaining suffix equals a roster
     * full name exactly. No fuzzy matching — an ambiguous or unknown name (chair titles,
     * ministers who are not MPs, guests) stays unmatched and keeps only speaker_raw.
     */
    public static Optional<PlenaryMember> matchSpeaker(
            String speakerRaw, Map<String, List<PlenaryMember>> rosterByFullName) {
        if (speakerRaw == null || speakerRaw.isBlank()) return Optional.empty();
        String[] tokens = speakerRaw.trim().split("\\s+");
        // A candidate name needs at least two tokens, so stop dropping before that.
        for (int drop = 0; drop <= tokens.length - 2; drop++) {
            String candidate = String.join(" ", List.of(tokens).subList(drop, tokens.length));
            List<PlenaryMember> hits = rosterByFullName.get(candidate);
            if (hits != null) {
                return hits.size() == 1 ? Optional.of(hits.get(0)) : Optional.empty();
            }
        }
        return Optional.empty();
    }
}
