package com.politico.vote;

import com.politico.AbstractIntegrationTest;
import com.politico.support.EntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class VoteEventRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private VoteEventRepository repo;

    private Instant t0;

    @BeforeEach
    void seed() {
        repo.deleteAll();
        t0 = Instant.parse("2026-01-01T10:00:00Z");
        repo.saveAll(List.of(
                EntityFactory.voteEvent("v-old", t0.minus(30, ChronoUnit.DAYS), VoteEventType.OPEN),
                EntityFactory.voteEvent("v-mid", t0, VoteEventType.ATTENDANCE_CHECK),
                EntityFactory.voteEvent("v-new", t0.plus(1, ChronoUnit.DAYS), VoteEventType.OPEN),
                EntityFactory.voteEvent("v-newer", t0.plus(2, ChronoUnit.DAYS), VoteEventType.SECRET)
        ));
    }

    @Test
    void search_with_no_filters_returns_all_ordered_desc() {
        // Regression cover for the null-cast fix: passing every filter as null
        // must not blow up the JPQL translation on real PostgreSQL.
        Page<VoteEvent> page = repo.search(null, null, null, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent())
                .extracting(VoteEvent::getExternalId)
                .containsExactly("v-newer", "v-new", "v-mid", "v-old");
    }

    @Test
    void search_with_from_only() {
        Page<VoteEvent> page = repo.search(t0, null, null, PageRequest.of(0, 10));
        assertThat(page.getContent())
                .extracting(VoteEvent::getExternalId)
                .containsExactly("v-newer", "v-new", "v-mid");
    }

    @Test
    void search_with_to_only() {
        Page<VoteEvent> page = repo.search(null, t0, null, PageRequest.of(0, 10));
        assertThat(page.getContent())
                .extracting(VoteEvent::getExternalId)
                .containsExactly("v-mid", "v-old");
    }

    @Test
    void search_with_type_filter() {
        Page<VoteEvent> page = repo.search(null, null, VoteEventType.OPEN, PageRequest.of(0, 10));
        assertThat(page.getContent())
                .extracting(VoteEvent::getExternalId)
                .containsExactly("v-new", "v-old");
    }

    @Test
    void search_paginates() {
        Page<VoteEvent> firstPage = repo.search(null, null, null, PageRequest.of(0, 2));
        assertThat(firstPage.getNumberOfElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent().get(0).getExternalId()).isEqualTo("v-newer");

        Page<VoteEvent> secondPage = repo.search(null, null, null, PageRequest.of(1, 2));
        assertThat(secondPage.getContent())
                .extracting(VoteEvent::getExternalId)
                .containsExactly("v-mid", "v-old");
    }

    @Test
    void search_combines_all_filters() {
        Page<VoteEvent> page = repo.search(
                t0.minus(60, ChronoUnit.DAYS),
                t0.plus(5, ChronoUnit.DAYS),
                VoteEventType.OPEN,
                PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
