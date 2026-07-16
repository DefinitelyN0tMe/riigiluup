package com.politico.ingestion.riigikogu;

import com.politico.vote.VoteEvent;
import com.politico.vote.VoteEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class VoteEventMapperTest {

    private final VoteEventMapper mapper = new VoteEventMapper();

    @Test
    void maps_open_roll_call_summary_from_sitting() {
        VotingListDto.VotingSummary summary = new VotingListDto.VotingSummary(
                "v-1", 104156,
                new VotingListDto.CodeValue("AVALIK", "Avalik"),
                "Päevakorra kinnitamine",
                "2026-06-01T15:05:57.183", "2026-06-01T15:06:17.177",
                79, 22, 68, 5, 1, 27
        );
        VotingListDto sitting = new VotingListDto(
                "s-1", "Täiskogu korraline istung 1.06.2026",
                "2026-06-01T15:00:00", java.util.List.of(summary));

        VoteEvent v = mapper.toEntity(summary, sitting);

        assertThat(v.getExternalId()).isEqualTo("v-1");
        assertThat(v.getSourceName()).isEqualTo("riigikogu");
        assertThat(v.getVotingNumber()).isEqualTo(104156);
        assertThat(v.getType()).isEqualTo(VoteEventType.OPEN);
        assertThat(v.getTypeSourceCode()).isEqualTo("AVALIK");
        assertThat(v.getDescription()).isEqualTo("Päevakorra kinnitamine");
        assertThat(v.getSittingExternalId()).isEqualTo("s-1");
        assertThat(v.getSittingTitle()).isEqualTo("Täiskogu korraline istung 1.06.2026");
        // Source sends local Tallinn wall-clock; 2026-06-01 is EEST (UTC+3), so 15:05 → 12:05Z.
        assertThat(v.getStartedAt()).isEqualTo(Instant.parse("2026-06-01T12:05:57.183Z"));
        assertThat(v.getEndedAt()).isEqualTo(Instant.parse("2026-06-01T12:06:17.177Z"));
        assertThat(v.getResultInFavor()).isEqualTo(68);
        assertThat(v.getResultAgainst()).isEqualTo(5);
        assertThat(v.getResultAbstained()).isEqualTo(27);
        assertThat(v.getResultNeutral()).isEqualTo(1);
        assertThat(v.getResultPresent()).isEqualTo(79);
        assertThat(v.getResultAbsent()).isEqualTo(22);
    }

    @Test
    void maps_attendance_check_type_and_null_counts_default_to_zero() {
        VotingListDto.VotingSummary summary = new VotingListDto.VotingSummary(
                "v-2", 104155,
                new VotingListDto.CodeValue("KOHALOLEKU_KONTROLL", "Kohaloleku kontroll"),
                "Kohaloleku kontroll",
                "2026-06-01T15:00:16.203", "2026-06-01T15:00:46.2",
                73, 28, null, null, null, null
        );
        VotingListDto sitting = new VotingListDto(
                "s-1", "Täiskogu 1.06.2026",
                "2026-06-01T15:00:00", java.util.List.of(summary));

        VoteEvent v = mapper.toEntity(summary, sitting);

        assertThat(v.getType()).isEqualTo(VoteEventType.ATTENDANCE_CHECK);
        assertThat(v.getResultInFavor()).isZero();
        assertThat(v.getResultAgainst()).isZero();
        assertThat(v.getResultAbstained()).isZero();
        assertThat(v.getResultNeutral()).isZero();
        assertThat(v.getResultPresent()).isEqualTo(73);
        assertThat(v.getResultAbsent()).isEqualTo(28);
    }

    @Test
    void applyDetail_updates_result_counts_from_full_detail() {
        VoteEvent existing = VoteEvent.builder()
                .externalId("v-3").sourceName("riigikogu")
                .type(VoteEventType.OPEN).typeSourceCode("AVALIK")
                .resultInFavor(1).resultAgainst(0).resultAbstained(0)
                .resultNeutral(0).resultPresent(0).resultAbsent(0)
                .importedAt(Instant.now()).updatedAt(Instant.now())
                .build();
        VotingDetailDto dto = new VotingDetailDto(
                "v-3", 104200,
                new VotingListDto.CodeValue("AVALIK", "Avalik"),
                "Uus kirjeldus",
                "2026-06-02T10:00:00", "2026-06-02T10:00:20",
                80, 21, 70, 6, 2, 25,
                new VotingDetailDto.Sitting("s-2", "Uus istung"),
                java.util.List.of()
        );

        mapper.applyDetail(existing, dto);

        assertThat(existing.getResultInFavor()).isEqualTo(70);
        assertThat(existing.getDescription()).isEqualTo("Uus kirjeldus");
        assertThat(existing.getSittingExternalId()).isEqualTo("s-2");
    }
}
