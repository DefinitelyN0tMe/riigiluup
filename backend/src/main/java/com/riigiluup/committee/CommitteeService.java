package com.riigiluup.committee;

import com.riigiluup.group.Group;
import com.riigiluup.group.GroupMembership;
import com.riigiluup.group.GroupMembershipRepository;
import com.riigiluup.group.GroupRepository;
import com.riigiluup.group.GroupType;
import com.riigiluup.group.MembershipRole;
import com.riigiluup.initiative.InitiativeService;
import com.riigiluup.legislation.LegislativeItem;
import com.riigiluup.legislation.LegislativeItemRepository;
import com.riigiluup.person.PlenaryMember;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Assembles the committee list and detail read models from existing group / membership /
 * legislative-item / initiative data. No committee-specific tables exist; a committee is a
 * {@code group} row of type STANDING_COMMITTEE.
 */
@Service
@RequiredArgsConstructor
public class CommitteeService {

    private final GroupRepository groupRepo;
    private final GroupMembershipRepository membershipRepo;
    private final LegislativeItemRepository itemRepo;
    private final InitiativeService initiativeService;

    /** The 11 active standing committees as cards, with cheap aggregate counts. */
    @Cacheable("committees-list")
    @Transactional(readOnly = true)
    public List<CommitteeDto.ListItem> list() {
        return groupRepo.findByTypeAndActiveTrueOrderByName(GroupType.STANDING_COMMITTEE).stream()
                .map(g -> new CommitteeDto.ListItem(
                        g.getExternalId(), g.getName(), g.getShortName(), g.getColorHex(),
                        g.getSecretariatName(),
                        membershipRepo.countByGroupAndActiveTrue(g),
                        itemRepo.countByLeadingCommitteeExternalId(g.getExternalId()),
                        initiativeService.countByCommitteeIfAny(g.getId())))
                .toList();
    }

    /** Empty when the id is unknown, inactive, or not a standing committee. */
    @Transactional(readOnly = true)
    public Optional<CommitteeDto.Detail> detail(String externalId) {
        return groupRepo
                .findByExternalIdAndTypeAndActiveTrue(externalId, GroupType.STANDING_COMMITTEE)
                .map(this::toDetail);
    }

    private CommitteeDto.Detail toDetail(Group g) {
        List<CommitteeDto.Member> members = sortMembers(
                membershipRepo.findByGroupAndActiveTrueWithMemberFetch(g).stream()
                        .map(CommitteeService::toMember)
                        .toList());

        long total = itemRepo.countByLeadingCommitteeExternalId(g.getExternalId());
        List<CommitteeDto.BillRef> recent = itemRepo
                .findRecentByLeadingCommittee(g.getExternalId(), PageRequest.of(0, 10)).stream()
                .map(CommitteeService::toBillRef)
                .toList();

        return new CommitteeDto.Detail(
                g.getExternalId(), g.getName(), g.getShortName(), g.getColorHex(),
                g.getSecretariatName(),
                members,
                new CommitteeDto.LedBills(total, recent),
                initiativeService.byCommittee(g.getId()));
    }

    private static CommitteeDto.Member toMember(GroupMembership gm) {
        PlenaryMember p = gm.getPlenaryMember();
        return new CommitteeDto.Member(
                p.getSlug(), p.getFullName(), gm.getRole().name(), p.getFactionName());
    }

    private static CommitteeDto.BillRef toBillRef(LegislativeItem i) {
        return new CommitteeDto.BillRef(
                i.getId(), i.getMark(), i.getTitle(),
                i.getPhase() == null ? null : i.getPhase().name(), i.getInitiatedDate());
    }

    /** Chair -> vice-chair -> everyone else, alphabetical by name inside each rank. */
    static List<CommitteeDto.Member> sortMembers(List<CommitteeDto.Member> members) {
        return members.stream()
                .sorted(Comparator.comparingInt(CommitteeService::roleRank)
                        .thenComparing(CommitteeDto.Member::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static int roleRank(CommitteeDto.Member m) {
        if (MembershipRole.CHAIR.name().equals(m.role())) return 0;
        if (MembershipRole.VICE_CHAIR.name().equals(m.role())) return 1;
        return 2;
    }
}
