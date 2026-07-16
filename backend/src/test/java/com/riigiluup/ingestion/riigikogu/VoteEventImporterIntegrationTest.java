package com.riigiluup.ingestion.riigikogu;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.riigiluup.AbstractIntegrationTest;
import com.riigiluup.alignment.VoteFactionAlignment;
import com.riigiluup.alignment.VoteFactionAlignmentRepository;
import com.riigiluup.person.PlenaryMember;
import com.riigiluup.person.PlenaryMemberRepository;
import com.riigiluup.support.EntityFactory;
import com.riigiluup.support.WireMockSupport;
import com.riigiluup.vote.IndividualVoteRepository;
import com.riigiluup.vote.VoteEvent;
import com.riigiluup.vote.VoteEventRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

class VoteEventImporterIntegrationTest extends AbstractIntegrationTest {

    // Start WireMock in a static initializer so its port is available when
    // Spring resolves the @DynamicPropertySource during context refresh.
    private static final WireMockServer wireMock = WireMockSupport.newServer();

    @Autowired private VoteEventImporter importer;
    @Autowired private VoteEventRepository voteEventRepo;
    @Autowired private IndividualVoteRepository individualVoteRepo;
    @Autowired private PlenaryMemberRepository memberRepo;
    @Autowired private ImportRunLogRepository runLogRepo;
    @Autowired private VoteFactionAlignmentRepository alignmentRepo;

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
        alignmentRepo.deleteAll();
        individualVoteRepo.deleteAll();
        voteEventRepo.deleteAll();
        runLogRepo.deleteAll();
        memberRepo.deleteAll();
    }

    @Test
    void imports_two_vote_events_with_voters_and_alignments() {
        seedMembers();
        stubHappyPath();

        ImportRunLog run = importer.runWindow(
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 16));

        assertThat(run.getStatus()).isEqualTo("SUCCESS");
        assertThat(run.getRecordsUpserted()).isEqualTo(2);
        assertThat(voteEventRepo.count()).isEqualTo(2);
        assertThat(individualVoteRepo.count()).isEqualTo(5); // 3 + 2 voters
        assertThat(runLogRepo.count()).isGreaterThanOrEqualTo(1);

        VoteEvent v1 = voteEventRepo.findBySourceNameAndExternalId("riigikogu", "vot-1000")
                .orElseThrow();
        List<VoteFactionAlignment> aligns = alignmentRepo.findByVoteEvent(v1);
        assertThat(aligns).hasSize(2); // Reform + EKRE
        assertThat(aligns).allMatch(VoteFactionAlignment::isHasClearMajority);
    }

    @Test
    void per_record_transaction_rolls_back_on_detail_fetch_failure_but_run_continues() {
        seedMembers();
        // Summary lists both votes, but vot-2000 returns a 500 on detail —
        // this must not roll back vot-1000, and the run should still SUCCEED.
        wireMock.stubFor(get(urlPathEqualTo("/api/votings"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockSupport.fixture("fixtures/riigikogu/votings-list.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/votings/vot-1000"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockSupport.fixture(
                                "fixtures/riigikogu/voting-detail-vot-1000.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/votings/vot-2000"))
                .willReturn(aResponse().withStatus(500)));

        ImportRunLog run = importer.runWindow(
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 16));

        // vot-1000 commits with full detail + voters; vot-2000's detail fetch fails (500), so its
        // whole per-record transaction rolls back — no misleading vote event without voters is
        // left behind — while the run continues and finishes PARTIAL rather than failing outright.
        assertThat(run.getStatus()).isEqualTo("PARTIAL");
        assertThat(voteEventRepo.count()).isEqualTo(1);
        assertThat(individualVoteRepo.count()).isEqualTo(3);
        assertThat(voteEventRepo.findBySourceNameAndExternalId("riigikogu", "vot-1000"))
                .isPresent();
        assertThat(voteEventRepo.findBySourceNameAndExternalId("riigikogu", "vot-2000"))
                .isEmpty();
    }

    private void seedMembers() {
        PlenaryMember a = EntityFactory.member("mp-A", "Kaja", "Kallas", "Reformierakond", true);
        PlenaryMember b = EntityFactory.member("mp-B", "Anna", "Aavik", "Reformierakond", true);
        PlenaryMember c = EntityFactory.member("mp-C", "Mart", "Helme", "EKRE", true);
        memberRepo.saveAll(List.of(a, b, c));
    }

    private void stubHappyPath() {
        wireMock.stubFor(get(urlPathEqualTo("/api/votings"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockSupport.fixture("fixtures/riigikogu/votings-list.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/votings/vot-1000"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockSupport.fixture(
                                "fixtures/riigikogu/voting-detail-vot-1000.json"))));
        wireMock.stubFor(get(urlPathEqualTo("/api/votings/vot-2000"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockSupport.fixture(
                                "fixtures/riigikogu/voting-detail-vot-2000.json"))));
        // Fallback: any unmatched voting URL returns 404 so we notice missing stubs.
        wireMock.stubFor(get(urlPathMatching("/api/votings/.*"))
                .atPriority(10)
                .willReturn(aResponse().withStatus(404)));
    }
}
