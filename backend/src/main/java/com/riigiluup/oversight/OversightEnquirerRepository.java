package com.riigiluup.oversight;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OversightEnquirerRepository extends JpaRepository<OversightEnquirer, Long> {

    List<OversightEnquirer> findByOversightItemId(UUID oversightItemId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from OversightEnquirer e where e.oversightItemId = :itemId")
    void deleteByOversightItemId(@Param("itemId") UUID itemId);
}
