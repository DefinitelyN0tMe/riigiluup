package com.riigiluup.legislation;

import com.riigiluup.AbstractIntegrationTest;
import com.riigiluup.support.EntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class LegislativeItemRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LegislativeItemRepository repo;

    @BeforeEach
    void seed() {
        repo.deleteAll();
        repo.save(EntityFactory.legislativeItem(
                "d-100", "Riigikaitseseaduse muutmine",
                LegislationPhase.SUBMITTED, LocalDate.of(2026, 1, 10)));
        repo.save(EntityFactory.legislativeItem(
                "d-101", "Karistusseadustiku muutmine",
                LegislationPhase.IN_READINGS, LocalDate.of(2026, 2, 15)));
        repo.save(EntityFactory.legislativeItem(
                "d-102", "Perekonnaseaduse muutmise seadus",
                LegislationPhase.ADOPTED, LocalDate.of(2026, 3, 3)));
    }

    @Test
    void search_returns_newest_first() {
        Page<LegislativeItem> page = repo.search(null, null, null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent())
                .extracting(LegislativeItem::getExternalId)
                .containsExactly("d-102", "d-101", "d-100");
    }

    @Test
    void search_by_q_case_insensitive_like() {
        Page<LegislativeItem> page = repo.search("karistus", null, null, null, null, null, null, PageRequest.of(0, 10));
        assertThat(page.getContent())
                .singleElement()
                .satisfies(i -> assertThat(i.getExternalId()).isEqualTo("d-101"));
    }

    @Test
    void search_by_phase() {
        Page<LegislativeItem> page = repo.search(
                null, LegislationPhase.ADOPTED.name(), null, null, null, null, null, PageRequest.of(0, 10));
        assertThat(page.getContent())
                .singleElement()
                .satisfies(i -> assertThat(i.getExternalId()).isEqualTo("d-102"));
    }

    @Test
    void search_paginates() {
        Page<LegislativeItem> firstPage = repo.search(null, null, null, null, null, null, null, PageRequest.of(0, 2));
        assertThat(firstPage.getNumberOfElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }
}
