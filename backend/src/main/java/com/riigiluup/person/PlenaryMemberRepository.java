package com.riigiluup.person;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlenaryMemberRepository extends JpaRepository<PlenaryMember, UUID> {

    Optional<PlenaryMember> findBySourceNameAndExternalId(String sourceName, String externalId);

    Optional<PlenaryMember> findBySlug(String slug);

    /** Paginated iteration over active members ordered by lastName for stable slices. */
    @Query("select m from PlenaryMember m where m.active = true order by m.lastName asc, m.id asc")
    Slice<PlenaryMember> findActiveOrderByLastName(Pageable pageable);

    java.util.List<PlenaryMember> findByActiveTrueOrderByLastNameAscFirstNameAsc();

    @Query("""
        select m from PlenaryMember m
        where (:active is null or m.active = :active)
          and (:q is null or lower(m.fullName) like lower(concat('%', cast(:q as string), '%')))
          and (:faction is null or m.factionExternalId = cast(:faction as string))
        """)
    Page<PlenaryMember> searchByFaction(
            @Param("q") String q,
            @Param("faction") String faction,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("""
        select new com.riigiluup.api.PoliticianController$FactionOption(
            m.factionExternalId, m.factionName, count(m))
        from PlenaryMember m
        where m.active = true and m.factionExternalId is not null
        group by m.factionExternalId, m.factionName
        order by m.factionName
        """)
    java.util.List<com.riigiluup.api.PoliticianController.FactionOption>
        findDistinctFactionsForActiveMembers();
}
