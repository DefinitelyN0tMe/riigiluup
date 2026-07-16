package com.riigiluup.api;

import com.riigiluup.common.PhotoUrlRewriter;
import com.riigiluup.person.PlenaryMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PoliticianMapper {

    private final PhotoUrlRewriter photoUrlRewriter;

    public PoliticianDto toDto(PlenaryMember m) {
        return new PoliticianDto(
                m.getId(),
                m.getSlug(),
                m.getFullName(),
                m.getFirstName(),
                m.getLastName(),
                photoUrlRewriter.toProxyPath(m.getPhotoUrl()),
                m.getOfficialProfileUrl(),
                m.isActive(),
                m.getFactionName(),
                m.getExternalId(),
                "https://api.riigikogu.ee/api/plenary-members/" + m.getExternalId()
        );
    }
}
