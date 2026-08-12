package com.riigiluup.ingestion.riigikogu;

import com.riigiluup.person.PlenaryMember;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlenaryMemberDetailMapperTest {

    private final PlenaryMemberDetailMapper mapper = new PlenaryMemberDetailMapper();

    private static PlenaryMember blankMember(String id, String slug) {
        return PlenaryMember.builder()
                .externalId(id).sourceName("riigikogu")
                .firstName("A").lastName("B").fullName("A B").slug(slug).active(true)
                .importedAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    private static PlenaryMemberDetailDto.CodeValue cv(String code, String value) {
        return new PlenaryMemberDetailDto.CodeValue(code, value);
    }

    private static PlenaryMemberDetailDto.MembershipRoleItem role(String start, String end) {
        return new PlenaryMemberDetailDto.MembershipRoleItem(start, end, cv("LIIGE", "liige"));
    }

    private static PlenaryMemberDetailDto.MembershipSpan span(String start, String end) {
        return new PlenaryMemberDetailDto.MembershipSpan(
                "s-1", 15, start, end, cv("LIIGE", "liige"), cv("RIIGIKOGU_LIIGE", "Riigikogu liige"));
    }

    private static PlenaryMemberDetailDto.DistrictEntry district(int termNum, String code, String name) {
        return new PlenaryMemberDetailDto.DistrictEntry(termNum, cv(code, name));
    }

    private static PlenaryMemberDetailDto.GroupRef faction(String uuid, String name, String endDate) {
        return new PlenaryMemberDetailDto.GroupRef(
                uuid, name, cv("FRAKTSIOON", "fraktsioon"), true, span("2023-04-10", endDate));
    }

    private static PlenaryMemberDetailDto.GroupRef committee(String uuid, String name, String endDate) {
        return new PlenaryMemberDetailDto.GroupRef(
                uuid, name, cv("ALALINE_KOMISJON", "alaline komisjon"), true, span("2023-04-10", endDate));
    }

    private static PlenaryMemberDetailDto.Photo photo(String href) {
        return new PlenaryMemberDetailDto.Photo(
                "p-1", "aab.jpg", "jpg",
                new PlenaryMemberDetailDto.Photo.Links(
                        new PlenaryMemberDetailDto.Photo.Href(href),
                        new PlenaryMemberDetailDto.Photo.Href("/api/files/p-1")));
    }

    @Test
    void applies_detail_and_picks_current_term_faction_and_district() {
        PlenaryMember m = blankMember("id-1", "jaak-aab");
        PlenaryMemberDetailDto.Membership past = new PlenaryMemberDetailDto.Membership(
                14,
                List.of(faction("f-old", "Old faction", "2023-04-01")),
                List.of(),
                List.of(district(14, "OLD", "Old district")),
                List.of(role("2022-06-04", "2023-04-01"))
        );
        PlenaryMemberDetailDto.Membership current = new PlenaryMemberDetailDto.Membership(
                15,
                List.of(faction("f-new", "Reformierakonna fraktsioon", null)),
                List.of(committee("c-1", "Rahanduskomisjon", null)),
                List.of(district(15, "JARVA_JA_VILJANDIMAA", "Järva- ja Viljandimaa")),
                List.of(role("2023-04-01", null))
        );
        PlenaryMemberDetailDto dto = new PlenaryMemberDetailDto(
                "id-1", "Jaak", "Aab", "Jaak Aab",
                "jaak.aab@riigikogu.ee", "MALE", "1960-04-09", "<p>bio</p>", 3383,
                photo("/api/files/p-1/download"),
                List.of(past, current),
                List.of()
        );

        mapper.applyDetail(m, dto);

        assertThat(m.getEmail()).isEqualTo("jaak.aab@riigikogu.ee");
        assertThat(m.getGender()).isEqualTo("MALE");
        assertThat(m.getDateOfBirth()).isEqualTo(LocalDate.of(1960, 4, 9));
        assertThat(m.getBiographyHtml()).isEqualTo("<p>bio</p>");
        assertThat(m.getParliamentSeniorityDays()).isEqualTo(3383);
        assertThat(m.getPhotoUrl()).isEqualTo("https://api.riigikogu.ee/api/files/p-1/download");
        assertThat(m.getElectoralDistrict()).isEqualTo("Järva- ja Viljandimaa");
        assertThat(m.getFactionExternalId()).isEqualTo("f-new");
        assertThat(m.getFactionName()).isEqualTo("Reformierakonna fraktsioon");
    }

    @Test
    void currentTermCommittees_returns_only_open_ended_memberships_of_current_term() {
        PlenaryMemberDetailDto.Membership current = new PlenaryMemberDetailDto.Membership(
                15,
                List.of(),
                List.of(
                        committee("open", "Rahanduskomisjon", null),
                        committee("closed", "Majanduskomisjon", "2024-01-01")
                ),
                List.of(),
                List.of(role("2023-04-01", null))
        );
        PlenaryMemberDetailDto dto = new PlenaryMemberDetailDto(
                "id-2", "A", "B", "A B", null, null, null, null, null, null,
                List.of(current),
                List.of()
        );

        var open = mapper.currentTermCommittees(dto);

        assertThat(open).hasSize(1);
        assertThat(open.get(0).uuid()).isEqualTo("open");
    }

    @Test
    void former_MP_with_no_active_role_item_gets_nulls_and_no_committees() {
        PlenaryMemberDetailDto.Membership past = new PlenaryMemberDetailDto.Membership(
                14,
                List.of(faction("f-old", "Old", "2023-04-01")),
                List.of(committee("c-old", "Old committee", "2023-04-01")),
                List.of(district(14, "OLD", "Old district")),
                List.of(role("2022-06-04", "2023-04-01"))
        );
        PlenaryMemberDetailDto dto = new PlenaryMemberDetailDto(
                "id-3", "A", "B", "A B", null, null, null, null, null, null,
                List.of(past),
                List.of()
        );
        PlenaryMember m = blankMember("id-3", "a-b");

        mapper.applyDetail(m, dto);
        var committees = mapper.currentTermCommittees(dto);

        assertThat(m.getElectoralDistrict()).isNull();
        assertThat(m.getFactionExternalId()).isNull();
        assertThat(committees).isEmpty();
    }

    @Test
    void sanitizes_biography_html_on_ingest() {
        PlenaryMemberDetailDto dto = new PlenaryMemberDetailDto(
                "id-5", "A", "B", "A B", null, null, null,
                "<p onclick=\"steal()\">bio <b>bold</b></p>"
                        + "<script>alert(1)</script>"
                        + "<a href=\"javascript:alert(1)\">link</a>"
                        + "<a href=\"https://riigikogu.ee\">ok</a>",
                null, null, List.of(), List.of()
        );
        PlenaryMember m = blankMember("id-5", "a-b-5");

        mapper.applyDetail(m, dto);

        assertThat(m.getBiographyHtml())
                .doesNotContain("<script")
                .doesNotContain("onclick")
                .doesNotContain("javascript:")
                .contains("<b>bold</b>")
                .contains("https://riigikogu.ee");
    }

    @Test
    void null_biography_stays_null() {
        PlenaryMemberDetailDto dto = new PlenaryMemberDetailDto(
                "id-6", "A", "B", "A B", null, null, null, null, null, null, List.of(), List.of()
        );
        PlenaryMember m = blankMember("id-6", "a-b-6");

        mapper.applyDetail(m, dto);

        assertThat(m.getBiographyHtml()).isNull();
    }

    @Test
    void handles_missing_photo() {
        PlenaryMemberDetailDto dto = new PlenaryMemberDetailDto(
                "id-4", "A", "B", "A B", null, null, null, null, null,
                null, List.of(), List.of()
        );
        PlenaryMember m = blankMember("id-4", "a-b-4");

        mapper.applyDetail(m, dto);

        assertThat(m.getPhotoUrl()).isNull();
    }
}
