package com.politico.ingestion.riigikogu;

import com.politico.person.PlenaryMember;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlenaryMemberDetailMapperTest {

    private final PlenaryMemberDetailMapper mapper = new PlenaryMemberDetailMapper();

    @Test
    void applies_detail_fields_to_existing_member() {
        PlenaryMember existing = PlenaryMember.builder()
                .externalId("id-1").sourceName("riigikogu")
                .firstName("Jaak").lastName("Aab").fullName("Jaak Aab")
                .slug("jaak-aab").active(true)
                .importedAt(java.time.Instant.now()).updatedAt(java.time.Instant.now())
                .build();

        PlenaryMemberDetailDto.Photo photo = new PlenaryMemberDetailDto.Photo(
                "p-1", "aab.jpg", "jpg",
                new PlenaryMemberDetailDto.Photo.Links(
                        new PlenaryMemberDetailDto.Photo.Href("/api/files/p-1/download"),
                        new PlenaryMemberDetailDto.Photo.Href("/api/files/p-1")
                )
        );

        PlenaryMemberDetailDto dto = new PlenaryMemberDetailDto(
                "id-1", "Jaak", "Aab", "Jaak Aab",
                "jaak.aab@riigikogu.ee", "MALE", "1960-04-09",
                "<p>bio</p>", 3383,
                photo,
                List.of(new PlenaryMemberDetailDto.Committee(
                        "c-1", "Rahanduskomisjon", "esimees", true)),
                List.of(new PlenaryMemberDetailDto.ElectoralDistrict(
                        "d-1", "Järva- ja Viljandimaa")),
                new PlenaryMemberDetailDto.Faction("f-1", "Reformierakonna fraktsioon")
        );

        mapper.applyDetail(existing, dto);

        assertThat(existing.getEmail()).isEqualTo("jaak.aab@riigikogu.ee");
        assertThat(existing.getGender()).isEqualTo("MALE");
        assertThat(existing.getDateOfBirth()).isEqualTo(LocalDate.of(1960, 4, 9));
        assertThat(existing.getBiographyHtml()).isEqualTo("<p>bio</p>");
        assertThat(existing.getParliamentSeniorityDays()).isEqualTo(3383);
        assertThat(existing.getElectoralDistrict()).isEqualTo("Järva- ja Viljandimaa");
        assertThat(existing.getPhotoUrl())
                .isEqualTo("https://api.riigikogu.ee/api/files/p-1/download");
        assertThat(existing.getFactionExternalId()).isEqualTo("f-1");
        assertThat(existing.getFactionName()).isEqualTo("Reformierakonna fraktsioon");
    }

    @Test
    void handles_missing_photo_and_district() {
        PlenaryMember existing = PlenaryMember.builder()
                .externalId("id-2").sourceName("riigikogu")
                .firstName("A").lastName("B").fullName("A B").slug("a-b").active(true)
                .importedAt(java.time.Instant.now()).updatedAt(java.time.Instant.now())
                .build();

        PlenaryMemberDetailDto dto = new PlenaryMemberDetailDto(
                "id-2", "A", "B", "A B",
                null, null, null, null, null,
                null, List.of(), List.of(), null
        );

        mapper.applyDetail(existing, dto);

        assertThat(existing.getPhotoUrl()).isNull();
        assertThat(existing.getElectoralDistrict()).isNull();
        assertThat(existing.getDateOfBirth()).isNull();
    }

    @Test
    void chooses_first_electoral_district_when_multiple() {
        PlenaryMember existing = PlenaryMember.builder()
                .externalId("id-3").sourceName("riigikogu")
                .firstName("A").lastName("B").fullName("A B").slug("a-b-3").active(true)
                .importedAt(java.time.Instant.now()).updatedAt(java.time.Instant.now())
                .build();

        PlenaryMemberDetailDto dto = new PlenaryMemberDetailDto(
                "id-3", "A", "B", "A B", null, null, null, null, null, null,
                List.of(),
                List.of(
                        new PlenaryMemberDetailDto.ElectoralDistrict("d-a", "First"),
                        new PlenaryMemberDetailDto.ElectoralDistrict("d-b", "Second")
                ),
                null
        );

        mapper.applyDetail(existing, dto);

        assertThat(existing.getElectoralDistrict()).isEqualTo("First");
    }
}
