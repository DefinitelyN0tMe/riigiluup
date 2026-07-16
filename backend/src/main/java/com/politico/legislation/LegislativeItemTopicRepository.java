package com.politico.legislation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LegislativeItemTopicRepository
        extends JpaRepository<LegislativeItemTopic, UUID> {

    Optional<LegislativeItemTopic> findByLegislativeItemAndTopic(
            LegislativeItem item, Topic topic);

    List<LegislativeItemTopic> findByLegislativeItem(LegislativeItem item);

    // Bulk JPQL delete — see LegislativeStageRepository for the Hibernate action-queue
    // ordering that makes a derived deleteBy hit the unique constraint on re-imports.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LegislativeItemTopic t where t.legislativeItem = :item")
    void deleteByLegislativeItem(@Param("item") LegislativeItem item);
}
