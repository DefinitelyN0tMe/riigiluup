package com.riigiluup.initiative;

import com.riigiluup.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Public initiative API, seeded directly through the repositories (not the importer) so each
 * case is a minimal, purpose-built fixture rather than a slice of the live CSV.
 *
 * <p>Seed shape — all destination='parliament' except {@code municipal}:
 * <ul>
 *   <li>{@code decided} — signatureCount 1500 (above threshold), decision
 *       draft-act-or-national-matter, one committee link;</li>
 *   <li>{@code rejected} — signatureCount 1200 (above threshold), decision reject;</li>
 *   <li>{@code sentBelowThreshold} — signatureCount 500 (below threshold) but
 *       sentToParliamentAt is set — the funnel's non-monotonic/"honesty" case;</li>
 *   <li>{@code municipal} — destination=tallinn, must never surface in the public API.</li>
 * </ul>
 */
@AutoConfigureMockMvc
@Transactional
class InitiativeApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private InitiativeRepository initiativeRepo;
    @Autowired private InitiativeCommitteeLinkRepository linkRepo;

    private Initiative decided;

    @BeforeEach
    void seed() {
        linkRepo.deleteAll();
        initiativeRepo.deleteAll();

        Instant now = Instant.now();

        decided = initiativeRepo.save(Initiative.builder()
                .sourceName("rahvaalgatus")
                .externalId("it-decided")
                .uuid("uuid-decided")
                .title("Decided initiative")
                .authors("Jane Citizen")
                .destination("parliament")
                .phase(InitiativePhase.DONE)
                .publishedAt(now)
                .signingStartedAt(now)
                .signingEndsAt(now)
                .signatureCount(1500)
                .lastSignedAt(now)
                .sentToParliamentAt(now)
                .parliamentDecision(ParliamentDecision.DRAFT_ACT_OR_NATIONAL_MATTER)
                .finishedInParliamentAt(now)
                .importedAt(now)
                .build());
        linkRepo.save(InitiativeCommitteeLink.builder()
                .initiativeId(decided.getId())
                .committeeSlug("social-affairs")
                .groupId(null)
                .build());

        initiativeRepo.save(Initiative.builder()
                .sourceName("rahvaalgatus")
                .externalId("it-rejected")
                .uuid("uuid-rejected")
                .title("Rejected initiative")
                .authors("John Doe")
                .destination("parliament")
                .phase(InitiativePhase.DONE)
                .publishedAt(now)
                .signingStartedAt(now)
                .signingEndsAt(now)
                .signatureCount(1200)
                .lastSignedAt(now)
                .sentToParliamentAt(now)
                .parliamentDecision(ParliamentDecision.REJECT)
                .finishedInParliamentAt(now)
                .importedAt(now)
                .build());

        // Below the 1000 threshold, yet still sent — the funnel's non-monotonic honesty case.
        initiativeRepo.save(Initiative.builder()
                .sourceName("rahvaalgatus")
                .externalId("it-sent-below-threshold")
                .uuid("uuid-sent-below-threshold")
                .title("Sent below threshold")
                .authors("Ann Resident")
                .destination("parliament")
                .phase(InitiativePhase.PARLIAMENT)
                .publishedAt(now)
                .signingStartedAt(now)
                .signingEndsAt(now)
                .signatureCount(500)
                .lastSignedAt(now)
                .sentToParliamentAt(now)
                .importedAt(now)
                .build());

        initiativeRepo.save(Initiative.builder()
                .sourceName("rahvaalgatus")
                .externalId("it-municipal")
                .uuid("uuid-municipal")
                .title("Municipal initiative")
                .authors("Mari Maasikas")
                .destination("tallinn")
                .phase(InitiativePhase.SIGN)
                .publishedAt(now)
                .signingStartedAt(now)
                .signingEndsAt(now)
                .signatureCount(300)
                .lastSignedAt(now)
                .importedAt(now)
                .build());
    }

    @Test
    void list_countsOnlyParliamentDestinedInitiatives() throws Exception {
        mockMvc.perform(get("/api/v1/initiatives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(3)))
                .andExpect(jsonPath("$.items.length()", equalTo(3)));
    }

    @Test
    void list_filtersByDecision() throws Exception {
        mockMvc.perform(get("/api/v1/initiatives")
                        .param("decision", "draft-act-or-national-matter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.items[0].externalId", equalTo("it-decided")));
    }

    @Test
    void detail_returnsThresholdAndCommittees_forARealId() throws Exception {
        mockMvc.perform(get("/api/v1/initiatives/" + decided.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threshold", equalTo(1000)))
                .andExpect(jsonPath("$.committees.length()", equalTo(1)))
                .andExpect(jsonPath("$.committees[0].slug", equalTo("social-affairs")));
    }

    @Test
    void detail_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/initiatives/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void funnel_reflectsSeedCounts_includingSentBelowThresholdHonestyCounter() throws Exception {
        // Seed: 3 parliament initiatives (targeted=3), all with signing_started_at (signing=3),
        // 2 at/above the 1000 threshold (threshold=2), all 3 sent (sent=3), 2 decided
        // (decided=2), 1 of those decisions is draft-act-or-national-matter (draftAct=1),
        // and 1 sent despite being below threshold (sentBelowThreshold=1).
        mockMvc.perform(get("/api/v1/analytics/initiative-funnel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[?(@.key=='targeted')].count[0]", equalTo(3)))
                .andExpect(jsonPath("$.steps[?(@.key=='signing')].count[0]", equalTo(3)))
                .andExpect(jsonPath("$.steps[?(@.key=='threshold')].count[0]", equalTo(2)))
                .andExpect(jsonPath("$.steps[?(@.key=='sent')].count[0]", equalTo(3)))
                .andExpect(jsonPath("$.steps[?(@.key=='decided')].count[0]", equalTo(2)))
                .andExpect(jsonPath("$.steps[?(@.key=='draftAct')].count[0]", equalTo(1)))
                .andExpect(jsonPath("$.sentBelowThreshold", equalTo(1)))
                .andExpect(jsonPath("$.sentBelowThreshold", greaterThan(0)));
    }
}
