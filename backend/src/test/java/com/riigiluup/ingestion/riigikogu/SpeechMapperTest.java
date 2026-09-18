package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.person.PlenaryMember;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SpeechMapperTest {

    private final SpeechMapper mapper = new SpeechMapper();

    private static VerbatimDto.Event event(String type, String uuid, String speaker, String text) {
        return new VerbatimDto.Event(type, uuid, "2026-06-08T15:00:44.000+00:00", speaker, text, null);
    }

    private static PlenaryMember member(String id, String first, String last) {
        return PlenaryMember.builder()
                .externalId(id).sourceName("riigikogu")
                .firstName(first).lastName(last).fullName(first + " " + last)
                .slug((first + "-" + last).toLowerCase()).active(true)
                .importedAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    private static Map<String, List<PlenaryMember>> roster(PlenaryMember... members) {
        Map<String, List<PlenaryMember>> byName = new java.util.HashMap<>();
        for (PlenaryMember m : members) {
            byName.computeIfAbsent(m.getFullName(), k -> new java.util.ArrayList<>()).add(m);
        }
        return byName;
    }

    @Test
    void flattens_only_speech_events_with_text_and_parses_offset_datetime() {
        // NB: the event `uuid` in the source identifies the SPEAKER (person uuid), not the
        // event — verified against live data; the OpenAPI description is wrong. It may be
        // null for guests, and a null-uuid speech must still be kept.
        VerbatimDto v = new VerbatimDto(15, 7, "https://stenogrammid.riigikogu.ee/202606081500",
                "2026-06-08T12:00:00.000+00:00", "XV Riigikogu, VII istungjärk, täiskogu istung", false,
                List.of(new VerbatimDto.AgendaItem("ai-1", "2026-06-08T15:00:00.000+00:00",
                        "<p>Istungi <b>rakendamine</b></p>",
                        List.of(
                                event("SPEECH", "person-1", "Esimees Lauri Hussar", "Austatud Riigikogu!"),
                                event("VOTING_EVENT", "vt-1", null, null),
                                event("SPEECH", "person-2", "Kaja Kallas", "   "),
                                event("SPEECH", null, "Külaline Mari Mets", "Tänan kutse eest!"),
                                event("SESSION_END", "se-1", null, null)
                        ))));

        List<SpeechMapper.FlatSpeech> out = mapper.flatten(v);

        assertThat(out).hasSize(2);
        SpeechMapper.FlatSpeech s = out.get(0);
        assertThat(s.speakerUuid()).isEqualTo("person-1");
        assertThat(s.speakerRaw()).isEqualTo("Esimees Lauri Hussar");
        // The source's "15:00:44+00:00" is Tallinn wall-clock time (EEST in June), not UTC.
        assertThat(s.spokenAt()).isEqualTo(Instant.parse("2026-06-08T12:00:44Z"));
        assertThat(s.sittingTitle()).isEqualTo("XV Riigikogu, VII istungjärk, täiskogu istung");
        assertThat(s.sittingLink()).isEqualTo("https://stenogrammid.riigikogu.ee/202606081500");
        assertThat(s.agendaItemTitle()).isEqualTo("Istungi rakendamine");
        assertThat(s.text()).isEqualTo("Austatud Riigikogu!");
        assertThat(out.get(1).speakerUuid()).isNull();
        assertThat(out.get(1).text()).isEqualTo("Tänan kutse eest!");
    }

    @Test
    void matches_plain_name_and_role_prefixed_name_but_never_fuzzy() {
        PlenaryMember hussar = member("mp-1", "Lauri", "Hussar");
        PlenaryMember kallas = member("mp-2", "Kaja", "Kallas");
        Map<String, List<PlenaryMember>> roster = roster(hussar, kallas);

        assertThat(SpeechMapper.matchSpeaker("Kaja Kallas", roster)).contains(kallas);
        assertThat(SpeechMapper.matchSpeaker("Esimees Lauri Hussar", roster)).contains(hussar);
        assertThat(SpeechMapper.matchSpeaker("Peaminister Kristen Michal", roster)).isEmpty();
        assertThat(SpeechMapper.matchSpeaker("Lauri", roster)).isEmpty();
        assertThat(SpeechMapper.matchSpeaker(null, roster)).isEmpty();
    }

    @Test
    void ambiguous_full_name_stays_unmatched() {
        PlenaryMember a = member("mp-1", "Jaanus", "Tamm");
        PlenaryMember b = member("mp-2", "Jaanus", "Tamm");
        Map<String, List<PlenaryMember>> roster = roster(a, b);

        assertThat(SpeechMapper.matchSpeaker("Jaanus Tamm", roster)).isEmpty();
        assertThat(SpeechMapper.matchSpeaker("Aseesimees Jaanus Tamm", roster)).isEmpty();
    }
}
