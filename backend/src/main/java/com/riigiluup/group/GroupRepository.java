package com.riigiluup.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    Optional<Group> findBySourceNameAndExternalId(String sourceName, String externalId);
    List<Group> findByTypeAndActiveTrueOrderByName(GroupType type);
    Optional<Group> findByExternalIdAndTypeAndActiveTrue(String externalId, GroupType type);
    Optional<Group> findByExternalIdAndActiveTrue(String externalId);
}
