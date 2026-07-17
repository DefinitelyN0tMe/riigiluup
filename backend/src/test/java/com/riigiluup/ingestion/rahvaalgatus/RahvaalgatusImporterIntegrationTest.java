package com.riigiluup.ingestion.rahvaalgatus;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.riigiluup.AbstractIntegrationTest;
import com.riigiluup.group.Group;
import com.riigiluup.group.GroupRepository;
import com.riigiluup.group.GroupType;
import com.riigiluup.ingestion.riigikogu.ImportRunLog;
import com.riigiluup.ingestion.riigikogu.ImportRunLogRepository;
import com.riigiluup.initiative.Initiative;
import com.riigiluup.initiative.InitiativeCommitteeLink;
import com.riigiluup.initiative.InitiativeCommitteeLinkRepository;
import com.riigiluup.initiative.InitiativeRepository;
import com.riigiluup.support.WireMockSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers {@link RahvaalgatusImporter} against a small hand-made CSV fixture
 * (see {@code fixtures/rahvaalgatus/*.csv}) standing in for the real 1141-row export.
 */
class RahvaalgatusImporterIntegrationTest extends AbstractIntegrationTest {

    // Start WireMock in a static initializer so its port is available when
    // Spring resolves the @DynamicPropertySource during context refresh.
    private static final WireMockServer wireMock = WireMockSupport.newServer();

    @Autowired private RahvaalgatusImporter importer;
    @Autowired private InitiativeRepository initiativeRepo;
    @Autowired private InitiativeCommitteeLinkRepository linkRepo;
    @Autowired private GroupRepository groupRepo;
    @Autowired private ImportRunLogRepository runLogRepo;

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) wireMock.stop();
    }

    @DynamicPropertySource
    static void wireMockBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("riigiluup.rahvaalgatus.base-url",
                () -> "http://localhost:" + wireMock.port());
    }

    @BeforeEach
    void reset() {
        wireMock.resetAll();
        linkRepo.deleteAll();
        initiativeRepo.deleteAll();
        runLogRepo.deleteAll();
        groupRepo.deleteAll();
    }

    @Test
    void happyPath_importsAllDestinationsIncludingMunicipal_andKeepsBlankSignatureCountNull() {
        seedCommittees();
        stubInitiatives("fixtures/rahvaalgatus/initiatives.csv");

        ImportRunLog run = importer.runFullRefresh();

        assertThat(run.getStatus()).isEqualTo("SUCCESS");
        assertThat(run.getRecordsSeen()).isEqualTo(4);
        assertThat(run.getRecordsUpserted()).isEqualTo(4);
        assertThat(initiativeRepo.count()).isEqualTo(4);

        // Municipal (tallinn) initiative IS stored — filtering happens at read time, not import.
        Initiative municipal = initiativeRepo
                .findBySourceNameAndExternalId(RahvaalgatusClient.SOURCE_NAME, "103")
                .orElseThrow();
        assertThat(municipal.getDestination()).isEqualTo("tallinn");

        // Blank signature_count must stay null, never become 0.
        Initiative blankCount = initiativeRepo
                .findBySourceNameAndExternalId(RahvaalgatusClient.SOURCE_NAME, "104")
                .orElseThrow();
        assertThat(blankCount.getSignatureCount()).isNull();
        assertThat(blankCount.getSentToParliamentAt()).isNotNull();
    }

    @Test
    void multiCommittee_reconciliation_produces_two_links_both_resolved() {
        seedCommittees();
        stubInitiatives("fixtures/rahvaalgatus/initiatives.csv");

        importer.runFullRefresh();

        Initiative twoCommittees = initiativeRepo
                .findBySourceNameAndExternalId(RahvaalgatusClient.SOURCE_NAME, "102")
                .orElseThrow();
        List<InitiativeCommitteeLink> links = linkRepo.findByInitiativeId(twoCommittees.getId());

        assertThat(links).hasSize(2);
        assertThat(links).extracting(InitiativeCommitteeLink::getCommitteeSlug)
                .containsExactlyInAnyOrder("social-affairs", "environment");
        assertThat(links).allMatch(l -> l.getGroupId() != null);
    }

    @Test
    void rerun_isIdempotent_noDuplicateInitiativesOrCommitteeLinks() {
        seedCommittees();
        stubInitiatives("fixtures/rahvaalgatus/initiatives.csv");

        importer.runFullRefresh();
        long firstInitiativeCount = initiativeRepo.count();
        long firstLinkCount = linkRepo.count();

        importer.runFullRefresh();

        assertThat(initiativeRepo.count()).isEqualTo(firstInitiativeCount);
        assertThat(linkRepo.count()).isEqualTo(firstLinkCount);
    }

    @Test
    void committeeReconciliation_dropsStaleLinkAndKeepsCurrentSetOnChange() {
        seedCommittees();
        stubInitiatives("fixtures/rahvaalgatus/initiatives.csv");
        importer.runFullRefresh();

        Initiative twoCommittees = initiativeRepo
                .findBySourceNameAndExternalId(RahvaalgatusClient.SOURCE_NAME, "102")
                .orElseThrow();
        assertThat(linkRepo.findByInitiativeId(twoCommittees.getId())).hasSize(2);

        // Source correction: initiative 102 is no longer assigned to the environment committee.
        stubInitiatives("fixtures/rahvaalgatus/initiatives-committee-change.csv");
        importer.runFullRefresh();

        List<InitiativeCommitteeLink> linksAfterChange =
                linkRepo.findByInitiativeId(twoCommittees.getId());
        assertThat(linksAfterChange).hasSize(1);
        assertThat(linksAfterChange.get(0).getCommitteeSlug()).isEqualTo("social-affairs");
        assertThat(linksAfterChange.get(0).getGroupId()).isNotNull();
    }

    @Test
    void unknownPhase_failsTheRunLoudly() {
        seedCommittees();
        stubInitiatives("fixtures/rahvaalgatus/initiatives-bad-phase.csv");

        ImportRunLog run = importer.runFullRefresh();

        assertThat(run.getStatus()).isEqualTo("FAILED");
        assertThat(run.getErrorMessage()).contains("bogus-phase");
    }

    private void stubInitiatives(String fixturePath) {
        wireMock.stubFor(get(urlPathEqualTo("/initiatives"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "text/csv")
                        .withBody(WireMockSupport.fixture(fixturePath))));
    }

    /** Matches InitiativeCommittee.SOCIAL_AFFAIRS / .ENVIRONMENT committee names exactly. */
    private void seedCommittees() {
        Instant now = Instant.now();
        groupRepo.save(Group.builder()
                .externalId("committee-social-affairs")
                .sourceName("riigikogu")
                .type(GroupType.STANDING_COMMITTEE)
                .name("Sotsiaalkomisjon")
                .active(true)
                .importedAt(now)
                .updatedAt(now)
                .build());
        groupRepo.save(Group.builder()
                .externalId("committee-environment")
                .sourceName("riigikogu")
                .type(GroupType.STANDING_COMMITTEE)
                .name("Keskkonnakomisjon")
                .active(true)
                .importedAt(now)
                .updatedAt(now)
                .build());
    }
}
