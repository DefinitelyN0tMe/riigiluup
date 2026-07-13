package com.politico.person;

import com.politico.AbstractIntegrationTest;
import com.politico.support.EntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class PlenaryMemberRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PlenaryMemberRepository repo;

    @BeforeEach
    void wipe() {
        repo.deleteAll();
    }

    @Test
    void save_and_find_by_source_and_external_id() {
        PlenaryMember saved = repo.save(EntityFactory.member(
                "u-1", "Kaja", "Kallas", "Reformierakond", true));
        assertThat(saved.getId()).isNotNull();

        var found = repo.findBySourceNameAndExternalId("riigikogu", "u-1");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Kaja Kallas");
        assertThat(found.get().getFactionExternalId()).isEqualTo("F-Reformierakond");
    }

    @Test
    void find_by_slug_round_trip() {
        repo.save(EntityFactory.member("u-2", "Jüri", "Ratas", "Keskerakond", true));
        assertThat(repo.findBySlug("jüri-ratas-u-2")).isPresent();
    }

    @Test
    void search_paginates_and_filters_active_only() {
        repo.save(EntityFactory.member("u-10", "Anna", "Aavik", "Reformierakond", true));
        repo.save(EntityFactory.member("u-11", "Bruno", "Bergen", "Reformierakond", true));
        repo.save(EntityFactory.member("u-12", "Cara", "Colt", "Reformierakond", false));

        Page<PlenaryMember> page = repo.searchByFaction(
                null, null, true,
                PageRequest.of(0, 10, Sort.by("lastName")));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(PlenaryMember::getLastName)
                .containsExactly("Aavik", "Bergen");

        Page<PlenaryMember> all = repo.searchByFaction(
                null, null, false,
                PageRequest.of(0, 10, Sort.by("lastName")));
        assertThat(all.getTotalElements()).isEqualTo(3);
    }

    @Test
    void search_filters_by_q_case_insensitively() {
        repo.save(EntityFactory.member("u-20", "Kaja", "Kallas", "Reformierakond", true));
        repo.save(EntityFactory.member("u-21", "Mart", "Helme", "EKRE", true));

        Page<PlenaryMember> hits = repo.searchByFaction(
                "kall", null, true,
                PageRequest.of(0, 10, Sort.by("lastName")));

        assertThat(hits.getContent())
                .singleElement()
                .satisfies(m -> assertThat(m.getLastName()).isEqualTo("Kallas"));
    }

    @Test
    void search_filters_by_faction_external_id() {
        repo.save(EntityFactory.member("u-30", "Kaja", "Kallas", "Reformierakond", true));
        repo.save(EntityFactory.member("u-31", "Mart", "Helme", "EKRE", true));

        Page<PlenaryMember> hits = repo.searchByFaction(
                null, "F-EKRE", true,
                PageRequest.of(0, 10, Sort.by("lastName")));

        assertThat(hits.getTotalElements()).isEqualTo(1);
        assertThat(hits.getContent().get(0).getLastName()).isEqualTo("Helme");
    }

    @Test
    void unique_constraint_on_source_and_external_id_is_enforced() {
        repo.save(EntityFactory.member("dup", "One", "One", "F", true));
        assertThatThrownBy(() -> {
            repo.saveAndFlush(EntityFactory.member("dup", "Two", "Two", "F", true));
        }).isInstanceOfAny(DataIntegrityViolationException.class, Exception.class);
    }
}
