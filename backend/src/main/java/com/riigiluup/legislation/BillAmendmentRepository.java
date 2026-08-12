package com.riigiluup.legislation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BillAmendmentRepository extends JpaRepository<BillAmendment, UUID> {

    List<BillAmendment> findByLegislativeItemOrderBySequenceAsc(LegislativeItem item);

    long count();

    // Bulk delete flushed before the fresh inserts, same reasoning as LegislativeStageRepository.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from BillAmendment a where a.legislativeItem = :item")
    void deleteByLegislativeItem(@Param("item") LegislativeItem item);
}
