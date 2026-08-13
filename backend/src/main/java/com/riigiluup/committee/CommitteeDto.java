package com.riigiluup.committee;

import com.riigiluup.initiative.InitiativeDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Read models for the committee list and detail pages. */
public final class CommitteeDto {

    private CommitteeDto() {
    }

    /** One card on /committees. Counts are cheap aggregate queries, computed once (cached). */
    public record ListItem(
            String externalId,
            String name,
            String shortName,
            String colorHex,
            String secretariat,
            long memberCount,
            long ledBillCount,
            long initiativeCount,
            String kind          // STANDING | SPECIAL
    ) {
    }

    /** A seated member of a committee. slug -> /politicians/{slug}; faction is denormalised on the member. */
    public record Member(String slug, String name, String role, String factionName) {
    }

    /** A bill this committee leads. id -> /legislation/{id}. */
    public record BillRef(UUID id, Integer mark, String title, String phase, LocalDate initiatedDate) {
    }

    /** Recent led bills plus the full count, so "all N" links to the filtered legislation list. */
    public record LedBills(long total, List<BillRef> recent) {
    }

    public record Detail(
            String externalId,
            String name,
            String shortName,
            String colorHex,
            String secretariat,
            List<Member> members,
            LedBills ledBills,
            List<InitiativeDto.ListItem> initiatives,
            String kind          // STANDING | SPECIAL
    ) {
    }
}
