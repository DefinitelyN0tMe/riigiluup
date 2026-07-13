package com.politico.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    Optional<Group> findBySourceNameAndExternalId(String sourceName, String externalId);
    List<Group> findByTypeAndActiveTrueOrderByName(GroupType type);
}
