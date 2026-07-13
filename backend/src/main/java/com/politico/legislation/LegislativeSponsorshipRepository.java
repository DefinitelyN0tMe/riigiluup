package com.politico.legislation;

import com.politico.person.PlenaryMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LegislativeSponsorshipRepository
        extends JpaRepository<LegislativeSponsorship, UUID> {

    List<LegislativeSponsorship> findByLegislativeItem(LegislativeItem item);

    void deleteByLegislativeItem(LegislativeItem item);

    @Query("""
        select s.legislativeItem from LegislativeSponsorship s
        where s.plenaryMember = :member
        order by s.legislativeItem.initiatedDate desc nulls last
        """)
    Page<LegislativeItem> findItemsSponsoredByMember(
            @Param("member") PlenaryMember member, Pageable pageable);

    long countByPlenaryMember(PlenaryMember member);
}
