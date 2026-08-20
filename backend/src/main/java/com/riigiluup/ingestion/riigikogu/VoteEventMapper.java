package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.vote.VoteEvent;
import com.riigiluup.vote.VoteEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class VoteEventMapper {

    private static final String SOURCE = "riigikogu";

    public VoteEvent toEntity(VotingListDto.VotingSummary s, VotingListDto sitting) {
        Instant now = Instant.now();
        return VoteEvent.builder()
                .externalId(s.uuid())
                .sourceName(SOURCE)
                .votingNumber(s.votingNumber())
                .type(VoteEventType.fromSourceCode(s.type() == null ? null : s.type().code()))
                .typeSourceCode(s.type() == null ? null : s.type().code())
                .description(s.description())
                .sittingExternalId(sitting == null ? null : sitting.uuid())
                .sittingTitle(sitting == null ? null : sitting.title())
                .startedAt(parseTs(s.startDateTime()))
                .endedAt(parseTs(s.endDateTime()))
                .resultInFavor(nz(s.inFavor()))
                .resultAgainst(nz(s.against()))
                .resultAbstained(nz(s.abstained()))
                .resultNeutral(nz(s.neutral()))
                .resultPresent(nz(s.present()))
                .resultAbsent(nz(s.absent()))
                .importedAt(now)
                .updatedAt(now)
                .build();
    }

    public void applyDetail(VoteEvent target, VotingDetailDto d) {
        target.setVotingNumber(d.votingNumber());
        if (d.type() != null) {
            target.setType(VoteEventType.fromSourceCode(d.type().code()));
            target.setTypeSourceCode(d.type().code());
        }
        target.setDescription(d.description());
        if (d.sitting() != null) {
            target.setSittingExternalId(d.sitting().uuid());
            target.setSittingTitle(d.sitting().title());
        }
        target.setStartedAt(parseTs(d.startDateTime()));
        target.setEndedAt(parseTs(d.endDateTime()));
        target.setResultInFavor(nz(d.inFavor()));
        target.setResultAgainst(nz(d.against()));
        target.setResultAbstained(nz(d.abstained()));
        target.setResultNeutral(nz(d.neutral()));
        target.setResultPresent(nz(d.present()));
        target.setResultAbsent(nz(d.absent()));
        target.setUpdatedAt(Instant.now());
    }

    private static int nz(Integer v) { return v == null ? 0 : v; }

    // Riigikogu emits offset-less local Estonian wall-clock (e.g. "2026-06-01T15:00:00" = 15:00
    // Tallinn). Interpret it in the source zone so the stored absolute instant is correct; the
    // analytics layer then converts back with AT TIME ZONE 'Europe/Tallinn' for display buckets.
    private static final ZoneId SOURCE_ZONE = ZoneId.of("Europe/Tallinn");

    private static Instant parseTs(String s) {
        if (s == null || s.isBlank()) return null;
        // Normal case: offset-less local Estonian wall-clock -> interpret in the source zone.
        try {
            return LocalDateTime.parse(s).atZone(SOURCE_ZONE).toInstant();
        } catch (DateTimeParseException ignored) { /* fall through to offset-bearing forms */ }
        // Defensive fallbacks so a reformatted/offset-bearing timestamp isn't silently dropped:
        // a null started_at hides the vote from every analytics query AND (with no timestamp) it is
        // re-fetched on every 6-hourly run forever (the >14-day settled-skip needs a non-null date).
        try {
            return OffsetDateTime.parse(s).toInstant();
        } catch (DateTimeParseException ignored) { /* try a bare instant next */ }
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException e) {
            log.warn("vote timestamp unparseable, storing null (vote will re-fetch each run): '{}'", s);
            return null;
        }
    }
}
