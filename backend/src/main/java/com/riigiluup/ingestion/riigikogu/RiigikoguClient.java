package com.riigiluup.ingestion.riigikogu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Component
@Retry(name = "riigikogu")
@CircuitBreaker(name = "riigikogu")
public class RiigikoguClient {

    private static final String SOURCE_NAME = "riigikogu";
    // Bound every outbound call so a slow/hanging Riigikogu can't park a request thread (and, on
    // the profile path, a DB connection) indefinitely. Combined with @Retry these stay bounded.
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    private final RestClient rest;
    private final ObjectMapper mapper;
    private final RiigikoguProperties props;

    public RiigikoguClient(RiigikoguProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.rest = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, props.userAgent())
                .requestFactory(factory)
                .build();
    }

    public String sourceName() { return SOURCE_NAME; }

    public List<PlenaryMemberDto> fetchAllPlenaryMembers() {
        throttle();
        PlenaryMemberDto[] arr = rest.get()
                .uri("/api/plenary-members?lang=et")
                .retrieve()
                .body(PlenaryMemberDto[].class);
        return arr == null ? List.of() : List.of(arr);
    }

    public JsonNode fetchRawPlenaryMember(String uuid) {
        throttle();
        return rest.get()
                .uri("/api/plenary-members/{uuid}?lang=et", uuid)
                .retrieve()
                .body(JsonNode.class);
    }

    /** Nanos of the last outbound request; -1 = never. Guarded via synchronized on {@link #throttle()}. */
    private long lastRequestNanos = -1L;
    private static final long MIN_INTERVAL_NANOS = 1_050_000_000L; // 1.05 s = ~0.95 rps, safely under Riigikogu's 1 rps

    /**
     * Deficit-based single-token throttle, called at the top of every network method so that
     * every outbound HTTP request is rate-limited regardless of caller — and, because this is a
     * singleton bean with a synchronized monitor, concurrent callers (6-hourly refresh, historical
     * backfill, admin-triggered imports) serialize through it and share the same ≤1 rps budget.
     *
     * <p>Sleeps only long enough to keep the outbound rate ≤ 1 rps averaged over consecutive calls;
     * if MIN_INTERVAL_NANOS already elapsed, returns immediately. Not a bucket4j-style multi-token
     * bucket; burst tolerance is still deferred to when we need it.
     */
    private synchronized void throttle() {
        long now = System.nanoTime();
        if (lastRequestNanos >= 0) {
            long elapsed = now - lastRequestNanos;
            long sleepNanos = MIN_INTERVAL_NANOS - elapsed;
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        lastRequestNanos = System.nanoTime();
    }

    // --- Added in Phase 2 ---

    public List<UsergroupDto> fetchAllUsergroups() {
        throttle();
        UsergroupDto[] arr = rest.get()
                .uri("/api/usergroups?lang=et")
                .retrieve()
                .body(UsergroupDto[].class);
        return arr == null ? List.of() : List.of(arr);
    }

    public PlenaryMemberDetailDto fetchPlenaryMemberDetail(String uuid) {
        throttle();
        return rest.get()
                .uri("/api/plenary-members/{uuid}?lang=et", uuid)
                .retrieve()
                .body(PlenaryMemberDetailDto.class);
    }

    public JsonNode fetchParticipationStats(String memberUuid, LocalDate startDate, LocalDate endDate) {
        throttle();
        return rest.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/statistics/participations/member/{uuid}")
                        .queryParam("startDate", startDate.toString())
                        .queryParam("endDate", endDate.toString())
                        .queryParam("lang", "et")
                        .build(memberUuid))
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode fetchVotingStats(String memberUuid, LocalDate startDate, LocalDate endDate) {
        throttle();
        return rest.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/statistics/votings/member/{uuid}")
                        .queryParam("startDate", startDate.toString())
                        .queryParam("endDate", endDate.toString())
                        .queryParam("lang", "et")
                        .build(memberUuid))
                .retrieve()
                .body(JsonNode.class);
    }

    public byte[] fetchFileBytes(String fileUuid) {
        throttle();
        return rest.get()
                .uri("/api/files/{uuid}/download", fileUuid)
                .retrieve()
                .body(byte[].class);
    }

    public List<VotingListDto> fetchVotingsInWindow(java.time.LocalDate from, java.time.LocalDate to) {
        throttle();
        VotingListDto[] arr = rest.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/votings")
                        .queryParam("startDate", from.toString())
                        .queryParam("endDate", to.toString())
                        .queryParam("lang", "et")
                        .build())
                .retrieve()
                .body(VotingListDto[].class);
        return arr == null ? List.of() : List.of(arr);
    }

    public VotingDetailDto fetchVotingDetail(String votingUuid) {
        throttle();
        return rest.get()
                .uri("/api/votings/{uuid}?lang=et", votingUuid)
                .retrieve()
                .body(VotingDetailDto.class);
    }

    public DraftListDto fetchDraftsInWindow(java.time.LocalDate from, java.time.LocalDate to) {
        return fetchDraftsInWindow(from, to, 0, 100);
    }

    /**
     * Paginated variant. Riigikogu returns 20 items per page by default; a window
     * with heavy legislative activity (say Q2 of an election year) can easily
     * exceed that, so callers MUST iterate pages until {@code number == totalPages - 1}.
     * Requesting {@code size=100} cuts per-window HTTP roundtrips ~5x.
     */
    public DraftListDto fetchDraftsInWindow(java.time.LocalDate from, java.time.LocalDate to,
                                            int page, int size) {
        throttle();
        return rest.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/volumes/drafts")
                        .queryParam("startDate", from.toString())
                        .queryParam("endDate", to.toString())
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParam("lang", "et")
                        .build())
                .retrieve()
                .body(DraftListDto.class);
    }

    public DraftDetailDto fetchDraftDetail(String uuid) {
        throttle();
        return rest.get()
                .uri("/api/volumes/drafts/{uuid}?lang=et", uuid)
                .retrieve()
                .body(DraftDetailDto.class);
    }

    // --- MP activity (speeches, interpellations, written questions) ---

    /** Per-member plenary speech/question counts over a date range, in a single call (uuids CSV). */
    public List<SpeechCountDto> fetchSpeechCounts(List<String> uuids, LocalDate from, LocalDate to) {
        throttle();
        String csv = String.join(",", uuids);
        SpeechCountDto[] arr = rest.get()
                .uri(b -> b.path("/api/steno/speeches")
                        .queryParam("uuids", csv)
                        .queryParam("startDate", from.toString())
                        .queryParam("endDate", to.toString())
                        .queryParam("type", "IS")
                        .build())
                .retrieve()
                .body(SpeechCountDto[].class);
        return arr == null ? List.of() : List.of(arr);
    }

    /** Number of interpellations submitted by the given MP (HAL page.totalElements). */
    public int countInterpellations(String enquirerUuid) {
        throttle();
        JsonNode n = rest.get()
                .uri(b -> b.path("/api/volumes/interpellations")
                        .queryParam("enquirerUuid", enquirerUuid)
                        .queryParam("size", 1)
                        .queryParam("lang", "et")
                        .build())
                .retrieve()
                .body(JsonNode.class);
        return pageTotal(n);
    }

    /** Number of written questions submitted by the given MP (HAL page.totalElements). */
    public int countWrittenQuestions(String enquirerUuid) {
        throttle();
        JsonNode n = rest.get()
                .uri(b -> b.path("/api/volumes/written-questions")
                        .queryParam("enquirerUuid", enquirerUuid)
                        .queryParam("size", 1)
                        .queryParam("lang", "et")
                        .build())
                .retrieve()
                .body(JsonNode.class);
        return pageTotal(n);
    }

    private static int pageTotal(JsonNode n) {
        if (n == null) return 0;
        JsonNode p = n.path("page").path("totalElements");
        return p.isNumber() ? p.asInt() : n.path("totalElements").asInt(0);
    }
}
