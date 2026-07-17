package com.riigiluup.person;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MpPartyMembershipRepository extends JpaRepository<MpPartyMembership, Long> {

    /** Oldest first; Postgres puts NULL start_date last for ASC — undated periods sink to the end. */
    List<MpPartyMembership> findByMemberExternalIdOrderByStartDateAsc(String memberExternalId);

    /** Full refresh of the Wikidata-sourced rows, leaving any äriregister rows (phase 2) intact. */
    void deleteBySource(String source);
}
