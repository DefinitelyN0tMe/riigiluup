package com.politico.person;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlenaryMemberRepository extends JpaRepository<PlenaryMember, UUID> {

    Optional<PlenaryMember> findBySourceNameAndExternalId(String sourceName, String externalId);

    Optional<PlenaryMember> findBySlug(String slug);

    @Query("""
        select m from PlenaryMember m
        where (:activeOnly = false or m.active = true)
          and (:q is null or lower(m.fullName) like lower(concat('%', cast(:q as string), '%')))
          and (:faction is null or m.factionExternalId = cast(:faction as string))
        """)
    Page<PlenaryMember> searchByFaction(
            @Param("q") String q,
            @Param("faction") String faction,
            @Param("activeOnly") boolean activeOnly,
            Pageable pageable
    );

    @Query("""
        select new com.politico.api.PoliticianController$FactionOption(
            m.factionExternalId, m.factionName, count(m))
        from PlenaryMember m
        where m.active = true and m.factionExternalId is not null
        group by m.factionExternalId, m.factionName
        order by m.factionName
        """)
    java.util.List<com.politico.api.PoliticianController.FactionOption>
        findDistinctFactionsForActiveMembers();
}
