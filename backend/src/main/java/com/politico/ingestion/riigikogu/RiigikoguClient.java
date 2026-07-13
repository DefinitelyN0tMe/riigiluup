package com.politico.ingestion.riigikogu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Component
public class RiigikoguClient {

    private static final String SOURCE_NAME = "riigikogu";

    private final RestClient rest;
    private final ObjectMapper mapper;
    private final RiigikoguProperties props;

    public RiigikoguClient(RiigikoguProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.rest = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, props.userAgent())
                .build();
    }

    public String sourceName() { return SOURCE_NAME; }

    public List<PlenaryMemberDto> fetchAllPlenaryMembers() {
        PlenaryMemberDto[] arr = rest.get()
                .uri("/api/plenary-members?lang=et")
                .retrieve()
                .body(PlenaryMemberDto[].class);
        return arr == null ? List.of() : List.of(arr);
    }

    public JsonNode fetchRawPlenaryMember(String uuid) {
        return rest.get()
                .uri("/api/plenary-members/{uuid}?lang=et", uuid)
                .retrieve()
                .body(JsonNode.class);
    }

    /**
     * Simple sleep between requests to respect the 1 rps limit.
     * A proper token-bucket goes to a Phase 2 task.
     */
    public void throttle() {
        try {
            Thread.sleep(Duration.ofMillis(1100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --- Added in Phase 2 ---

    public List<UsergroupDto> fetchAllUsergroups() {
        UsergroupDto[] arr = rest.get()
                .uri("/api/usergroups?lang=et")
                .retrieve()
                .body(UsergroupDto[].class);
        return arr == null ? List.of() : List.of(arr);
    }

    public PlenaryMemberDetailDto fetchPlenaryMemberDetail(String uuid) {
        return rest.get()
                .uri("/api/plenary-members/{uuid}?lang=et", uuid)
                .retrieve()
                .body(PlenaryMemberDetailDto.class);
    }

    public JsonNode fetchParticipationStats(String memberUuid, LocalDate startDate, LocalDate endDate) {
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
        return rest.get()
                .uri("/api/files/{uuid}/download", fileUuid)
                .retrieve()
                .body(byte[].class);
    }

    public List<VotingListDto> fetchVotingsInWindow(java.time.LocalDate from, java.time.LocalDate to) {
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
        return rest.get()
                .uri("/api/votings/{uuid}?lang=et", votingUuid)
                .retrieve()
                .body(VotingDetailDto.class);
    }
}
