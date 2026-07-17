package com.riigiluup.initiative;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InitiativeRepository extends JpaRepository<Initiative, Long> {

    Optional<Initiative> findBySourceNameAndExternalId(String sourceName, String externalId);

    /** Reverse link for the bill page: "this act started as a citizen initiative". */
    List<Initiative> findByLegislativeItemId(UUID legislativeItemId);
}
