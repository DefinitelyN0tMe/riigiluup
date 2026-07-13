package com.politico.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BackfillRunRepository extends JpaRepository<BackfillRun, UUID> {

    Optional<BackfillRun> findFirstByStatusOrderByStartedAtDesc(String status);

    Optional<BackfillRun> findFirstByOrderByStartedAtDesc();
}
