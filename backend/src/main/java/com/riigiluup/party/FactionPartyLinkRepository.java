package com.riigiluup.party;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FactionPartyLinkRepository extends JpaRepository<FactionPartyLink, UUID> {
    Optional<FactionPartyLink> findFirstByFactionExternalIdOrderByValidFromDesc(
            String factionExternalId
    );
}
