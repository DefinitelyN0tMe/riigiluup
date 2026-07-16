package com.riigiluup.ingestion.riigikogu;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.riigiluup.AbstractIntegrationTest;
import com.riigiluup.legislation.LegislativeItem;
import com.riigiluup.legislation.LegislativeItemRepository;
import com.riigiluup.legislation.LegislativeItemTopicRepository;
import com.riigiluup.legislation.LegislativeSponsorshipRepository;
import com.riigiluup.legislation.LegislativeStageRepository;
import com.riigiluup.legislation.SponsorKind;
import com.riigiluup.legislation.TopicRepository;
import com.riigiluup.support.WireMockSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class LegislativeItemImporterIntegrationTest extends AbstractIntegrationTest {

    private static final WireMockServer wireMock = WireMockSupport.newServer();

    @Autowired private LegislativeItemImporter importer;
    @Autowired private LegislativeItemRepository itemRepo;
    @Autowired private LegislativeStageRepository stageRepo;
    @Autowired private LegislativeSponsorshipRepository sponsorshipRepo;
    @Autowired private LegislativeItemTopicRepository itemTopicRepo;
    @Autowired private TopicRepository topicRepo;
    @Autowired private ImportRunLogRepository runLogRepo;

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) wireMock.stop();
    }

    @DynamicPropertySource
    static void wireMockBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("riigiluup.riigikogu.base-url",
                () -> "http://localhost:" + wireMock.port());
    }

    @BeforeEach
    void reset() {
        wireMock.resetAll();
        itemTopicRepo.deleteAll();
        sponsorshipRepo.deleteAll();
        stageRepo.deleteAll();
        itemRepo.deleteAll();
        topicRepo.deleteAll();
        runLogRepo.deleteAll();
    }

    @Test
    void imports_two_drafts_with_stages_sponsorships_and_topics() {
        wireMock.stubFor(get(urlPathEqualTo("/api/volumes/drafts"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockSupport.fixture("fixtures/riigikogu/drafts-list.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/volumes/drafts/drf-1"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockSupport.fixture(
                                "fixtures/riigikogu/draft-detail-drf-1.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/volumes/drafts/drf-2"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockSupport.fixture(
                                "fixtures/riigikogu/draft-detail-drf-2.json"))));

        ImportRunLog run = importer.runWindow(
                LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 10));

        assertThat(run.getStatus()).isEqualTo("SUCCESS");
        assertThat(run.getRecordsUpserted()).isEqualTo(2);
        assertThat(itemRepo.count()).isEqualTo(2);

        LegislativeItem drf1 = itemRepo
                .findBySourceNameAndExternalId("riigikogu", "drf-1")
                .orElseThrow();
        assertThat(drf1.getTitle()).contains("Karistusseadustiku");
        // drf-1 has 1 reading with 1 event → 1 stage
        assertThat(stageRepo.count()).isGreaterThanOrEqualTo(3); // 1 + 2
        // drf-1 initiators: 1 MP + 1 faction group → 2 sponsorships
        // drf-2 initiators: 1 gov (classifier) → 1 sponsorship
        assertThat(sponsorshipRepo.count()).isEqualTo(3);
        // Topics: 2 for drf-1 + 1 for drf-2
        assertThat(topicRepo.count()).isEqualTo(3);
        assertThat(itemTopicRepo.count()).isEqualTo(3);

        assertThat(sponsorshipRepo.findAll())
                .extracting(s -> s.getSponsorKind())
                .contains(SponsorKind.PLENARY_MEMBER, SponsorKind.FACTION, SponsorKind.ORGAN);
    }

    @Test
    void rerun_upserts_without_duplication() {
        wireMock.stubFor(get(urlPathEqualTo("/api/volumes/drafts"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockSupport.fixture("fixtures/riigikogu/drafts-list.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/volumes/drafts/drf-1"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockSupport.fixture(
                                "fixtures/riigikogu/draft-detail-drf-1.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/volumes/drafts/drf-2"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockSupport.fixture(
                                "fixtures/riigikogu/draft-detail-drf-2.json"))));

        importer.runWindow(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 10));
        long firstItemCount = itemRepo.count();
        long firstTopicCount = topicRepo.count();

        importer.runWindow(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 10));

        assertThat(itemRepo.count()).isEqualTo(firstItemCount);
        // Topics are upserted by (source, edid) — reruns MUST NOT duplicate.
        assertThat(topicRepo.count()).isEqualTo(firstTopicCount);
    }
}
