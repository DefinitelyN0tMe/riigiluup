package com.politico.group;

import com.politico.person.PlenaryMember;
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

    @Query("""
        select gm from GroupMembership gm
        join fetch gm.group
        where gm.plenaryMember = :member and gm.active = true
        """)
    List<GroupMembership> findByPlenaryMemberAndActiveTrueWithGroupFetch(@Param("member") PlenaryMember member);
}
