package com.riigiluup.person;

import com.riigiluup.AbstractIntegrationTest;
import com.riigiluup.support.EntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Party memberships (Wikidata P102) surfaced on the politician profile — seeded directly.
 */
@AutoConfigureMockMvc
@Transactional
class PartyMembershipProfileIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PlenaryMemberRepository memberRepo;
    @Autowired private MpPartyMembershipRepository partyRepo;

    private PlenaryMember mp;

    @BeforeEach
    void seed() {
        partyRepo.deleteAll();
        memberRepo.deleteAll();

        mp = memberRepo.save(EntityFactory.member("mp-p1", "Test", "Member", "Reform", true));
        Instant now = Instant.now();
        partyRepo.save(MpPartyMembership.builder()
                .memberExternalId(mp.getExternalId())
                .partyQid("Q1").partyLabel("Old Party")
                .startDate(LocalDate.of(2010, 1, 1)).endDate(LocalDate.of(2018, 6, 1))
                .source("wikidata").importedAt(now).build());
        partyRepo.save(MpPartyMembership.builder()
                .memberExternalId(mp.getExternalId())
                .partyQid("Q2").partyLabel("New Party")
                .startDate(LocalDate.of(2018, 6, 2)).endDate(null)
                .source("wikidata").importedAt(now).build());
    }

    @Test
    void profile_returnsPartyMemberships_oldestFirst() throws Exception {
        mockMvc.perform(get("/api/v1/politicians/" + mp.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partyMemberships.length()", equalTo(2)))
                .andExpect(jsonPath("$.partyMemberships[0].partyLabel", equalTo("Old Party")))
                .andExpect(jsonPath("$.partyMemberships[0].endDate", equalTo("2018-06-01")))
                .andExpect(jsonPath("$.partyMemberships[1].partyLabel", equalTo("New Party")));
    }

    @Test
    void profile_withNoMemberships_returnsEmptyArray() throws Exception {
        PlenaryMember other = memberRepo.save(EntityFactory.member("mp-p2", "No", "Party", "Reform", true));
        mockMvc.perform(get("/api/v1/politicians/" + other.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partyMemberships.length()", equalTo(0)));
    }
}
