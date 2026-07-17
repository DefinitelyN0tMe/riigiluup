package com.riigiluup.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Tallinn");

    private final AnalyticsService service;
    private final com.riigiluup.initiative.InitiativeService initiativeService;

    @GetMapping("/faction-agreement")
    public AnalyticsDto.FactionAgreementMatrix factionAgreement(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        Instant fromTs = from == null ? null : from.atStartOfDay(DISPLAY_ZONE).toInstant();
        Instant toTs = to == null ? null : to.plusDays(1).atStartOfDay(DISPLAY_ZONE).toInstant();
        return service.factionAgreement(fromTs, toTs);
    }

    @GetMapping("/member-activity")
    public AnalyticsDto.MemberActivityBoard memberActivity() {
        return service.memberActivity();
    }

    @GetMapping("/elections")
    public AnalyticsDto.ElectionBoard elections() {
        return service.elections();
    }

    @GetMapping("/party-finance")
    public AnalyticsDto.PartyFinanceBoard partyFinance() {
        return service.partyFinance();
    }

    @GetMapping("/response-latency")
    public AnalyticsDto.ResponseLatencyBoard responseLatency() {
        return service.responseLatency();
    }

    @GetMapping("/discipline-breakers")
    public AnalyticsDto.DisciplineBreakers disciplineBreakers(
            @RequestParam(defaultValue = "24") int limit,
            @RequestParam(defaultValue = "20") int minEligible
    ) {
        return service.disciplineBreakers(Math.min(limit, 200), Math.max(minEligible, 5));
    }

    @GetMapping("/bill-flow")
    public AnalyticsDto.BillFlow billFlow() {
        return service.billFlow();
    }

    @GetMapping("/attendance-matrix")
    public AnalyticsDto.AttendanceMatrix attendanceMatrix(
            @RequestParam(defaultValue = "40") int sittings
    ) {
        return service.attendanceMatrix(Math.min(sittings, 120));
    }

    @GetMapping("/vote-timing")
    public AnalyticsDto.VoteTimingHeatmap voteTiming() {
        return service.voteTiming();
    }

    @GetMapping("/topic-treemap")
    public AnalyticsDto.TopicTreemap topicTreemap(
            @RequestParam(defaultValue = "24") int limit
    ) {
        return service.topicTreemap(Math.min(limit, 60));
    }

    @GetMapping("/bill-velocity")
    public AnalyticsDto.BillVelocity billVelocity() {
        return service.billVelocity();
    }

    @GetMapping("/mp-similarity")
    public AnalyticsDto.MpSimilarity mpSimilarity() {
        return service.mpSimilarity();
    }

    @GetMapping("/co-sponsorship")
    public AnalyticsDto.CoSponsorship coSponsorship(
            @RequestParam(defaultValue = "2") int minWeight
    ) {
        return service.coSponsorship(Math.max(minWeight, 1));
    }

    @GetMapping("/highlights")
    public AnalyticsDto.HighlightsBundle highlights() {
        return service.highlights();
    }

    @GetMapping("/night-votes")
    public AnalyticsDto.NightVotes nightVotes(
            @RequestParam(defaultValue = "8") int startHour,
            @RequestParam(defaultValue = "22") int endHour,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return service.nightVotes(startHour, endHour, Math.min(limit, 100));
    }

    @GetMapping("/mp-deviations-timeline/{slug}")
    public AnalyticsDto.MpDeviationsTimeline mpDeviationsTimeline(
            @PathVariable String slug,
            @RequestParam(defaultValue = "12") int months
    ) {
        return service.mpDeviationsTimeline(slug, months);
    }

    @GetMapping("/mp-similar-peers/{slug}")
    public AnalyticsDto.MpSimilarPeers mpSimilarPeers(
            @PathVariable String slug,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "10") int minOverlap
    ) {
        return service.mpSimilarPeers(slug, limit, minOverlap);
    }

    @GetMapping("/mp-topic-radar/{slug}")
    public ResponseEntity<AnalyticsDto.MpTopicRadar> mpTopicRadar(
            @PathVariable String slug,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(service.mpTopicRadar(slug, Math.min(limit, 20)));
    }

    @GetMapping("/initiative-funnel")
    public com.riigiluup.initiative.InitiativeDto.Funnel initiativeFunnel() {
        return initiativeService.funnel();
    }
}
