package com.politico.ingestion.riigikogu;

import com.politico.group.Group;
import com.politico.group.GroupType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsergroupMapperTest {

    private final UsergroupMapper mapper = new UsergroupMapper();

    @Test
    void maps_faction_dto_with_short_name_and_color() {
        UsergroupDto dto = new UsergroupDto(
                "f-1", "Reformierakonna fraktsioon", "RE", true,
                "#F9C812", "Kantselei",
                new UsergroupDto.Type("fraktsioon", "Fraktsioon")
        );

        Group g = mapper.toEntity(dto);

        assertThat(g.getExternalId()).isEqualTo("f-1");
        assertThat(g.getSourceName()).isEqualTo("riigikogu");
        assertThat(g.getType()).isEqualTo(GroupType.FRACTION);
        assertThat(g.getName()).isEqualTo("Reformierakonna fraktsioon");
        assertThat(g.getShortName()).isEqualTo("RE");
        assertThat(g.getColorHex()).isEqualTo("#F9C812");
        assertThat(g.getSecretariatName()).isEqualTo("Kantselei");
        assertThat(g.isActive()).isTrue();
    }

    @Test
    void maps_standing_committee_with_null_optional_fields() {
        UsergroupDto dto = new UsergroupDto(
                "c-1", "Rahanduskomisjon", null, true, null, null,
                new UsergroupDto.Type("alaline_komisjon", "Alaline komisjon")
        );

        Group g = mapper.toEntity(dto);

        assertThat(g.getType()).isEqualTo(GroupType.STANDING_COMMITTEE);
        assertThat(g.getShortName()).isNull();
        assertThat(g.getColorHex()).isNull();
        assertThat(g.isActive()).isTrue();
    }

    @Test
    void unknown_type_becomes_OTHER() {
        UsergroupDto dto = new UsergroupDto(
                "x-1", "Whatever", null, false, null, null,
                new UsergroupDto.Type("something_new", "Uus")
        );

        Group g = mapper.toEntity(dto);

        assertThat(g.getType()).isEqualTo(GroupType.OTHER);
        assertThat(g.isActive()).isFalse();
    }

    @Test
    void null_type_defaults_to_OTHER() {
        UsergroupDto dto = new UsergroupDto(
                "n-1", "No type", null, true, null, null, null
        );

        Group g = mapper.toEntity(dto);

        assertThat(g.getType()).isEqualTo(GroupType.OTHER);
    }
}
