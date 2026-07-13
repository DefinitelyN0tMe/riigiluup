package com.politico.api;

import com.politico.common.PageResponse;
import com.politico.person.PlenaryMember;
import com.politico.person.PlenaryMemberRepository;
import com.politico.vote.IndividualVote;
import com.politico.vote.IndividualVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/politicians/{slug}/votes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PoliticianVotesController {

    private final PlenaryMemberRepository memberRepo;
    private final IndividualVoteRepository individualVoteRepo;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<Item>> list(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        PlenaryMember m = memberRepo.findBySlug(slug).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();
        Page<IndividualVote> p = individualVoteRepo.findByMemberChronological(
                m, PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(PageResponse.of(p.map(Item::of)));
    }

    public record Item(
            UUID voteEventId,
            String voteEventExternalId,
            String description,
            String type,
            Instant startedAt,
            String choice,
            String choiceSourceCode
    ) {
        static Item of(IndividualVote iv) {
            var ev = iv.getVoteEvent();
            return new Item(
                    ev == null ? null : ev.getId(),
                    ev == null ? null : ev.getExternalId(),
                    ev == null ? null : ev.getDescription(),
                    ev == null || ev.getType() == null ? null : ev.getType().name(),
                    ev == null ? null : ev.getStartedAt(),
                    iv.getChoice() == null ? "UNKNOWN" : iv.getChoice().name(),
                    iv.getChoiceSourceCode()
            );
        }
    }
}
