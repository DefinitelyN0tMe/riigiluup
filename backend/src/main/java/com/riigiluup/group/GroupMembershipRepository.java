package com.riigiluup.group;

import com.riigiluup.person.PlenaryMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, UUID> {

    Optional<GroupMembership> findByPlenaryMemberAndGroupAndStartDate(
            PlenaryMember member, Group group, java.time.LocalDate startDate
    );

    List<GroupMembership> findByPlenaryMemberAndActiveTrue(PlenaryMember member);

    List<GroupMembership> findByGroupAndActiveTrue(Group group);

    long countByGroupAndActiveTrue(Group group);

    /** Active memberships of a given group type across all MPs — used to gate the one-time backfill. */
    @Query("select count(gm) from GroupMembership gm where gm.active = true and gm.group.type = :type")
    long countActiveByType(@Param("type") GroupType type);

    @Query("""
        select gm from GroupMembership gm
        join fetch gm.plenaryMember
        where gm.group = :group and gm.active = true
        """)
    List<GroupMembership> findByGroupAndActiveTrueWithMemberFetch(@Param("group") Group group);

    @Query("""
        select gm from GroupMembership gm
        join fetch gm.group
        where gm.plenaryMember = :member and gm.active = true
        """)
    List<GroupMembership> findByPlenaryMemberAndActiveTrueWithGroupFetch(@Param("member") PlenaryMember member);
}
