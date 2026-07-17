package com.riigiluup.api;

import com.riigiluup.speech.SpeechRepository;
import com.riigiluup.speech.SpeechRepository.SpeechSearchRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Full-text search over plenary speeches. Matching is exact-wordform ('simple' config —
 * PostgreSQL has no Estonian stemmer); excerpts mark hits with [[ ]] delimiters that the
 * frontend converts to elements itself.
 */
@RestController
@RequestMapping("/api/v1/speeches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SpeechController {

    private static final ZoneId TALLINN = ZoneId.of("Europe/Tallinn");
    private static final int MAX_PAGE_SIZE = 50;

    private final SpeechRepository speechRepo;

    public record SpeechItem(Long id, String speakerRaw, String memberSlug, String memberName,
                             Instant spokenAt, String sittingTitle, String agendaItemTitle,
                             String excerpt, String sourceUrl) {}

    public record SpeechPage(List<SpeechItem> items, int page, int totalPages, long totalElements) {}

    @GetMapping
    @Transactional(readOnly = true)
    public SpeechPage search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String member,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String query = q == null || q.isBlank() ? null : q.trim();
        String slug = member == null || member.isBlank() ? null : member.trim();
        // Day bounds in the source's own timezone, upper bound exclusive.
        Instant fromTs = from == null ? null : from.atStartOfDay(TALLINN).toInstant();
        Instant toTs = to == null ? null : to.plusDays(1).atStartOfDay(TALLINN).toInstant();
        PageRequest pageable = PageRequest.of(
                Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE));

        Page<SpeechSearchRow> result = speechRepo.search(query, slug, fromTs, toTs, pageable);
        return new SpeechPage(
                result.getContent().stream().map(r -> new SpeechItem(
                        r.getId(), r.getSpeakerRaw(), r.getMemberSlug(), r.getMemberName(),
                        r.getSpokenAt(), r.getSittingTitle(), r.getAgendaItemTitle(),
                        r.getExcerpt(), r.getSourceUrl())).toList(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }
}
