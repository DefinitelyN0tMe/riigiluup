package com.riigiluup.party;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExternalAffiliationRepository extends JpaRepository<ExternalAffiliation, UUID> {
    /** Ordered oldest → newest so timelines render left-to-right. */
    List<ExternalAffiliation> findByMemberSlugOrderByValidFromAsc(String memberSlug);
}
