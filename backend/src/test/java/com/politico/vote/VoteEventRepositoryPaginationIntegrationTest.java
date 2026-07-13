package com.politico.vote;

import com.politico.AbstractIntegrationTest;
import com.politico.legislation.LegislationPhase;
import com.politico.legislation.LegislativeItem;
import com.politico.legislation.LegislativeItemRepository;
import com.politico.support.EntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the new paginated repo methods added to eliminate {@code findAll()} hot-loop
 * scans in the alignment backfill and vote-bill linker.
 */
@Transactional
class VoteEventRepositoryPaginationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private VoteEventRepository voteEventRepo;
    @Autowired private LegislativeItemRepository itemRepo;

    @BeforeEach
    void seed() {
        voteEventRepo.deleteAll();
        itemRepo.deleteAll();
    }

    @Test
    void findAllByStartedAtAsc_iterates_in_pages_ordered() {
        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
        // 5 events, unordered on insert.
        voteEventRepo.saveAll(List.of(
                EntityFactory.voteEvent("v-b", t0.plus(2, ChronoUnit.DAYS), VoteEventType.OPEN),
                EntityFactory.voteEvent("v-a", t0.plus(1, ChronoUnit.DAYS), VoteEventType.OPEN),
                EntityFactory.voteEvent("v-e", t0.plus(5, ChronoUnit.DAYS), VoteEventType.OPEN),
                EntityFactory.voteEvent("v-d", t0.plus(4, ChronoUnit.DAYS), VoteEventType.OPEN),
                EntityFactory.voteEvent("v-c", t0.plus(3, ChronoUnit.DAYS), VoteEventType.OPEN)
        ));

        Slice<VoteEvent> first = voteEventRepo.findAllByStartedAtAsc(PageRequest.of(0, 2));
        assertThat(first.getContent()).extracting(VoteEvent::getExternalId)
                .containsExactly("v-a", "v-b");
        assertThat(first.hasNext()).isTrue();

        Slice<VoteEvent> second = voteEventRepo.findAllByStartedAtAsc(PageRequest.of(1, 2));
        assertThat(second.getContent()).extracting(VoteEvent::getExternalId)
                .containsExactly("v-c", "v-d");
        assertThat(second.hasNext()).isTrue();

        Slice<VoteEvent> third = voteEventRepo.findAllByStartedAtAsc(PageRequest.of(2, 2));
        assertThat(third.getContent()).extracting(VoteEvent::getExternalId)
                .containsExactly("v-e");
        assertThat(third.hasNext()).isFalse();
    }

    @Test
    void findUnlinkedByStartedAtAsc_excludes_events_with_legislative_item() {
        Instant t0 = Instant.parse("2026-02-01T10:00:00Z");
        LegislativeItem bill = itemRepo.save(EntityFactory.legislativeItem(
                "bill-1", "Some Act", LegislationPhase.SUBMITTED, LocalDate.of(2026, 1, 1)));

        VoteEvent unlinked1 = EntityFactory.voteEvent("v-u1", t0, VoteEventType.OPEN);
        VoteEvent linked = EntityFactory.voteEvent("v-l", t0.plus(1, ChronoUnit.HOURS),
                VoteEventType.OPEN);
        linked.setLegislativeItem(bill);
        VoteEvent unlinked2 = EntityFactory.voteEvent("v-u2",
                t0.plus(2, ChronoUnit.HOURS), VoteEventType.OPEN);
        voteEventRepo.saveAll(List.of(unlinked1, linked, unlinked2));

        Slice<VoteEvent> slice = voteEventRepo.findUnlinkedByStartedAtAsc(
                PageRequest.of(0, 10));

        assertThat(slice.getContent()).extracting(VoteEvent::getExternalId)
                .containsExactly("v-u1", "v-u2");
        assertThat(slice.hasNext()).isFalse();
    }
}
