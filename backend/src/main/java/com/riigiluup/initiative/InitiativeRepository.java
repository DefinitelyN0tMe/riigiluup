package com.riigiluup.initiative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InitiativeRepository extends JpaRepository<Initiative, Long> {

    /** All initiative ids for the sitemap. */
    @org.springframework.data.jpa.repository.Query("select i.id from Initiative i")
    java.util.List<Long> findAllIdsForSitemap();

    Optional<Initiative> findBySourceNameAndExternalId(String sourceName, String externalId);

    /** Reverse link for the bill page: "this act started as a citizen initiative". */
    List<Initiative> findByLegislativeItemId(UUID legislativeItemId);

    /** Parliament-addressed initiatives assigned to a committee (by its group id). */
    @Query("""
        select i from Initiative i, InitiativeCommitteeLink l
        where l.initiativeId = i.id and l.groupId = :groupId
          and i.destination = 'parliament'
        order by i.sentToParliamentAt desc nulls last
        """)
    List<Initiative> findByCommitteeGroupId(@Param("groupId") UUID groupId);

    @Query("""
        select count(i) from Initiative i, InitiativeCommitteeLink l
        where l.initiativeId = i.id and l.groupId = :groupId
          and i.destination = 'parliament'
        """)
    long countByCommitteeGroupId(@Param("groupId") UUID groupId);
}
