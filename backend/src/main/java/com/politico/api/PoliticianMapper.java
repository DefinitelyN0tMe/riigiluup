package com.politico.api;

import com.politico.person.PlenaryMember;
import org.springframework.stereotype.Component;

@Component
public class PoliticianMapper {
    public PoliticianDto toDto(PlenaryMember m) {
        return new PoliticianDto(
                m.getId(),
                m.getSlug(),
                m.getFullName(),
                m.getFirstName(),
                m.getLastName(),
                m.getPhotoUrl(),
                m.getOfficialProfileUrl(),
                m.isActive(),
                m.getFactionName(),
                m.getExternalId(),
                "https://api.riigikogu.ee/api/plenary-members/" + m.getExternalId()
        );
    }
}
