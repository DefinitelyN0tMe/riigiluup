package com.riigiluup.group;

import com.riigiluup.person.PlenaryMember;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Assembles the parliamentary-groups directory from existing group / group-membership data — no new
 * tables. Covers the three "extra affiliation" group types (friendship groups, topic support groups,
 * delegations); committees and fractions have their own pages and are excluded here.
 */
@Service
@RequiredArgsConstructor
public class GroupDirectoryService {

    private static final List<GroupType> DIRECTORY_TYPES =
            List.of(GroupType.BILATERAL_GROUP, GroupType.ASSOCIATION, GroupType.DELEGATION);

    private final GroupRepository groupRepo;
    private final GroupMembershipRepository membershipRepo;

    /** All active directory groups with their live member counts, alphabetical within each category. */
    @Cacheable("group-directory-list")
    @Transactional(readOnly = true)
    public List<GroupDirectoryDto.ListItem> list() {
        return DIRECTORY_TYPES.stream()
                .flatMap(type -> groupRepo.findByTypeAndActiveTrueOrderByName(type).stream())
                .map(g -> new GroupDirectoryDto.ListItem(
                        g.getExternalId(),
                        g.getName(),
                        GroupDirectoryDto.categoryOf(g.getType()),
                        membershipRepo.countByGroupAndActiveTrue(g)))
                .filter(li -> li.memberCount() > 0)
                .toList();
    }

    /** Empty when the id is unknown, inactive, or not one of the directory group types. */
    @Cacheable("group-directory-detail")
    @Transactional(readOnly = true)
    public Optional<GroupDirectoryDto.Detail> detail(String externalId) {
        Optional<Group> found = groupRepo.findByExternalIdAndActiveTrue(externalId)
                .filter(g -> GroupDirectoryDto.categoryOf(g.getType()) != null);
        return found.map(g -> new GroupDirectoryDto.Detail(
                g.getExternalId(),
                g.getName(),
                GroupDirectoryDto.categoryOf(g.getType()),
                membershipRepo.findByGroupAndActiveTrueWithMemberFetch(g).stream()
                        .map(GroupDirectoryService::toMember)
                        .sorted(Comparator.comparing(GroupDirectoryDto.Member::name,
                                String.CASE_INSENSITIVE_ORDER))
                        .toList()));
    }

    private static GroupDirectoryDto.Member toMember(GroupMembership gm) {
        PlenaryMember p = gm.getPlenaryMember();
        return new GroupDirectoryDto.Member(p.getSlug(), p.getFullName(), p.getFactionName());
    }
}
