package com.politico.legislation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LegislativeItemRepository extends JpaRepository<LegislativeItem, UUID> {

    Optional<LegislativeItem> findBySourceNameAndExternalId(String sourceName, String externalId);

    @Query("""
        select i from LegislativeItem i
        where (:q is null
                or lower(i.title) like lower(concat('%', cast(:q as string), '%')))
          and (:phase is null or i.phase = :phase)
          and (:membership is null or i.membership = :membership)
        order by i.initiatedDate desc nulls last, i.mark desc nulls last
        """)
    Page<LegislativeItem> search(
            @Param("q") String q,
            @Param("phase") LegislationPhase phase,
            @Param("membership") Integer membership,
            Pageable pageable
    );
}
