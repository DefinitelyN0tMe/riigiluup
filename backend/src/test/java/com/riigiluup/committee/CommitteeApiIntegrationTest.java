package com.riigiluup.committee;

import com.riigiluup.AbstractIntegrationTest;
import com.riigiluup.group.Group;
import com.riigiluup.group.GroupMembership;
import com.riigiluup.group.GroupMembershipRepository;
import com.riigiluup.group.GroupRepository;
import com.riigiluup.group.GroupType;
import com.riigiluup.group.MembershipRole;
import com.riigiluup.initiative.Initiative;
import com.riigiluup.initiative.InitiativeCommitteeLink;
import com.riigiluup.initiative.InitiativeCommitteeLinkRepository;
import com.riigiluup.initiative.InitiativePhase;
import com.riigiluup.initiative.InitiativeRepository;
import com.riigiluup.legislation.LegislationPhase;
import com.riigiluup.legislation.LegislativeItem;
import com.riigiluup.legislation.LegislativeItemRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Committee API, seeded directly through the repositories. One active standing committee with a
 * chair and a member, a bill it leads, and an assigned parliament initiative; plus an inactive
 * standing committee that must never surface.
 */
@AutoConfigureMockMvc
@Transactional
class CommitteeApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private GroupRepository groupRepo;
    @Autowired private GroupMembershipRepository membershipRepo;
    @Autowired private PlenaryMemberRepository memberRepo;
    @Autowired private LegislativeItemRepository itemRepo;
    @Autowired private InitiativeRepository initiativeRepo;
    @Autowired private InitiativeCommitteeLinkRepository linkRepo;

    @BeforeEach
    void seed() {
        linkRepo.deleteAll();
        membershipRepo.deleteAll();
        initiativeRepo.deleteAll();
        itemRepo.deleteAll();
        memberRepo.deleteAll();
        groupRepo.deleteAll();

        Instant now = Instant.now();

        Group committee = groupRepo.save(Group.builder()
                .externalId("COMTEST")
                .sourceName("riigikogu")
                .type(GroupType.STANDING_COMMITTEE)
                .name("Test Committee")
                .shortName("TC")
                .colorHex("#123456")
                .secretariatName("Test secretariat")
                .active(true)
                .importedAt(now)
                .updatedAt(now)
                .build());

        // Inactive standing committee — must never appear in the list.
        groupRepo.save(Group.builder()
                .externalId("COMOLD")
                .sourceName("riigikogu")
                .type(GroupType.STANDING_COMMITTEE)
                .name("Former Committee")
                .active(false)
                .importedAt(now)
                .updatedAt(now)
                .build());

        PlenaryMember chair = memberRepo.save(EntityFactory.member("mp-c", "Cara", "Chair", "Ref", true));
        PlenaryMember member = memberRepo.save(EntityFactory.member("mp-m", "Mona", "Member", "Ref", true));

        membershipRepo.save(GroupMembership.builder()
                .plenaryMember(member).group(committee).role(MembershipRole.MEMBER)
                .active(true).importedAt(now).updatedAt(now).build());
        membershipRepo.save(GroupMembership.builder()
                .plenaryMember(chair).group(committee).role(MembershipRole.CHAIR)
                .active(true).importedAt(now).updatedAt(now).build());

        LegislativeItem bill = EntityFactory.legislativeItem(
                "bill-1", "A bill this committee leads", LegislationPhase.ADOPTED, LocalDate.of(2024, 1, 1));
        bill.setLeadingCommitteeExternalId("COMTEST");
        itemRepo.save(bill);

        Initiative initiative = initiativeRepo.save(Initiative.builder()
                .sourceName("rahvaalgatus").externalId("ini-1").title("An assigned initiative")
                .destination("parliament").phase(InitiativePhase.DONE)
                .signatureCount(1500).sentToParliamentAt(now).importedAt(now).build());
        linkRepo.save(InitiativeCommitteeLink.builder()
                .initiativeId(initiative.getId()).committeeSlug("legal-affairs")
                .groupId(committee.getId()).build());
    }

    @Test
    void list_returnsOnlyActiveStandingCommittees_withCounts() throws Exception {
        mockMvc.perform(get("/api/v1/committees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", equalTo(1)))
                .andExpect(jsonPath("$[0].externalId", equalTo("COMTEST")))
                .andExpect(jsonPath("$[0].memberCount", equalTo(2)))
                .andExpect(jsonPath("$[0].ledBillCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].initiativeCount", greaterThanOrEqualTo(1)));
    }

    @Test
    void detail_returnsMembersChairFirst_bills_andInitiatives() throws Exception {
        mockMvc.perform(get("/api/v1/committees/COMTEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo("Test Committee")))
                .andExpect(jsonPath("$.members.length()", equalTo(2)))
                .andExpect(jsonPath("$.members[0].role", equalTo("CHAIR")))
                .andExpect(jsonPath("$.ledBills.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.initiatives.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void detail_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/committees/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void detail_inactiveCommittee_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/committees/COMOLD"))
                .andExpect(status().isNotFound());
    }

    @Test
    void legislation_filtersByLeadingCommittee() throws Exception {
        mockMvc.perform(get("/api/v1/legislation").param("committee", "COMTEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }
}
