package com.politico.api;

import java.util.UUID;

public record PoliticianDto(
        UUID id,
        String slug,
        String fullName,
        String firstName,
        String lastName,
        String photoUrl,
        String officialProfileUrl,
        boolean active,
        String factionName,
        String externalId,
        String sourceUrl
) {}
