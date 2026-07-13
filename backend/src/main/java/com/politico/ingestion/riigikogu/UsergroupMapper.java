package com.politico.ingestion.riigikogu;

import com.politico.group.Group;
import com.politico.group.GroupType;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UsergroupMapper {

    public Group toEntity(UsergroupDto dto) {
        Instant now = Instant.now();
        String code = dto.type() == null ? null : dto.type().code();
        return Group.builder()
                .externalId(dto.uuid())
                .sourceName("riigikogu")
                .type(GroupType.fromSourceCode(code))
                .name(dto.name())
                .shortName(dto.shortName())
                .colorHex(dto.colorHex())
                .secretariatName(dto.secretariatName())
                .active(Boolean.TRUE.equals(dto.active()))
                .importedAt(now)
                .updatedAt(now)
                .build();
    }
}
