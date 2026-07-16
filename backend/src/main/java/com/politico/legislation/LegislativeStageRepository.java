package com.politico.legislation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LegislativeStageRepository extends JpaRepository<LegislativeStage, UUID> {
    List<LegislativeStage> findByLegislativeItemOrderBySequenceAsc(LegislativeItem item);

    // Bulk JPQL delete + flushAutomatically so the DELETE hits the DB BEFORE the fresh
    // stages are INSERTed. A derived deleteBy defers deletes to Hibernate's action queue
    // which flushes INSERTs first → duplicate-key on (item_id, sequence).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LegislativeStage s where s.legislativeItem = :item")
    void deleteByLegislativeItem(@Param("item") LegislativeItem item);
}
