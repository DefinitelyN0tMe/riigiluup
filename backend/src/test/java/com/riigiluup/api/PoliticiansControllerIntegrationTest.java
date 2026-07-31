package com.riigiluup.api;

import com.riigiluup.AbstractIntegrationTest;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.support.EntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class PoliticiansControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlenaryMemberRepository memberRepo;

    private PlenaryMember kaja;

    @BeforeEach
    void seed() {
        memberRepo.deleteAll();
        kaja = memberRepo.save(EntityFactory.member(
                "mp-001", "Kaja", "Kallas", "Reformierakond", true));
        memberRepo.save(EntityFactory.member(
                "mp-002", "Mart", "Helme", "EKRE", true));
        memberRepo.save(EntityFactory.member(
                "mp-003", "Jüri", "Ratas", "Keskerakond", false));
    }

    @Test
    void list_returns_active_members_by_default() throws Exception {
        mockMvc.perform(get("/api/v1/politicians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(2)))
                .andExpect(jsonPath("$.items.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.items[0].slug").exists())
                .andExpect(jsonPath("$.items[0].fullName").exists());
    }

    @Test
    void list_can_include_inactive_members() throws Exception {
        mockMvc.perform(get("/api/v1/politicians").param("status", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(3)));
    }

    @Test
    void list_filters_by_q() throws Exception {
        mockMvc.perform(get("/api/v1/politicians").param("q", "kall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.items[0].lastName", equalTo("Kallas")));
    }

    @Test
    void list_paginates() throws Exception {
        mockMvc.perform(get("/api/v1/politicians")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.items.length()", equalTo(1)))
                .andExpect(jsonPath("$.totalPages", equalTo(2)));
    }

    @Test
    void get_by_slug_returns_profile_shape() throws Exception {
        mockMvc.perform(get("/api/v1/politicians/" + kaja.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug", equalTo(kaja.getSlug())))
                .andExpect(jsonPath("$.fullName", equalTo("Kaja Kallas")))
                .andExpect(jsonPath("$.externalId").doesNotExist()) // profile DTO uses different shape
                .andExpect(jsonPath("$.sourceUrl").exists());
    }

    @Test
    void get_by_unknown_slug_returns_404() throws Exception {
        mockMvc.perform(get("/api/v1/politicians/does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
