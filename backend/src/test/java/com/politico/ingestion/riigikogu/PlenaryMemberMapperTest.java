package com.politico.ingestion.riigikogu;

import com.politico.person.PlenaryMember;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlenaryMemberMapperTest {

    private final PlenaryMemberMapper mapper = new PlenaryMemberMapper();

    @Test
    void maps_full_dto_to_entity() {
        PlenaryMemberDto dto = new PlenaryMemberDto(
                "abcd-1234", "Kaja", "Kallas", "Kaja Kallas",
                true,
                new PlenaryMemberDto.Faction("f-1", "Reformierakonna fraktsioon"),
                "https://cdn/kaja.jpg"
        );

        PlenaryMember out = mapper.toEntity(dto);

        assertThat(out.getExternalId()).isEqualTo("abcd-1234");
        assertThat(out.getSourceName()).isEqualTo("riigikogu");
        assertThat(out.getFullName()).isEqualTo("Kaja Kallas");
        assertThat(out.getSlug()).isEqualTo("kaja-kallas");
        assertThat(out.getFactionExternalId()).isEqualTo("f-1");
        assertThat(out.getFactionName()).isEqualTo("Reformierakonna fraktsioon");
        assertThat(out.isActive()).isTrue();
        assertThat(out.getPhotoUrl()).isEqualTo("https://cdn/kaja.jpg");
        assertThat(out.getOfficialProfileUrl())
                .isEqualTo("https://www.riigikogu.ee/riigikogu-liikmed/liige/abcd-1234/");
    }

    @Test
    void handles_null_faction_and_derives_full_name_when_missing() {
        PlenaryMemberDto dto = new PlenaryMemberDto(
                "xy", "Jaan", "Tamm", null, false, null, null
        );

        PlenaryMember out = mapper.toEntity(dto);

        assertThat(out.getFullName()).isEqualTo("Jaan Tamm");
        assertThat(out.getFactionExternalId()).isNull();
        assertThat(out.getFactionName()).isNull();
        assertThat(out.isActive()).isFalse();
        assertThat(out.getSlug()).isEqualTo("jaan-tamm");
    }

    @Test
    void slug_handles_estonian_diacritics() {
        PlenaryMemberDto dto = new PlenaryMemberDto(
                "z9", "Jüri", "Ratas", "Jüri Ratas", true, null, null
        );

        PlenaryMember out = mapper.toEntity(dto);

        assertThat(out.getSlug()).isEqualTo("juri-ratas");
    }
}
