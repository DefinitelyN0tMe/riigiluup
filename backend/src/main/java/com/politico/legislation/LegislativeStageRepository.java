package com.politico.legislation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LegislativeStageRepository extends JpaRepository<LegislativeStage, UUID> {
    List<LegislativeStage> findByLegislativeItemOrderBySequenceAsc(LegislativeItem item);
    void deleteByLegislativeItem(LegislativeItem item);
}
