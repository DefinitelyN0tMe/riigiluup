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

    @Query("""
        select m from PlenaryMember m
        where (:activeOnly = false or m.active = true)
          and (:q is null or lower(m.fullName) like lower(concat('%', cast(:q as string), '%')))
        """)
    Page<PlenaryMember> search(
            @Param("q") String q,
            @Param("activeOnly") boolean activeOnly,
            Pageable pageable
    );
}
