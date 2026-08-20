package com.riigiluup.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    /** (externalId, type) of active groups with a detail page, for the sitemap. */
    @org.springframework.data.jpa.repository.Query("select g.externalId, g.type from Group g where g.active = true and g.externalId is not null and g.type in :types")
    java.util.List<Object[]> findExternalIdAndTypeForSitemap(@org.springframework.data.repository.query.Param("types") java.util.Collection<GroupType> types);
    Optional<Group> findBySourceNameAndExternalId(String sourceName, String externalId);
    List<Group> findByTypeAndActiveTrueOrderByName(GroupType type);
    Optional<Group> findByExternalIdAndTypeAndActiveTrue(String externalId, GroupType type);
    Optional<Group> findByExternalIdAndActiveTrue(String externalId);
    /** Any group with this external id, active or dissolved — so a link to a now-dissolved committee
     *  still resolves to a read-only historical page instead of a 404. */
    Optional<Group> findFirstByExternalId(String externalId);
}
