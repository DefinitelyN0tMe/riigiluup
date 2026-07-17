package com.riigiluup.committee;

import com.riigiluup.group.MembershipRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommitteeServiceTest {

    private static CommitteeDto.Member m(String name, MembershipRole role) {
        return new CommitteeDto.Member(name.toLowerCase(), name, role.name(), "Faction");
    }

    /** Chair first, then vice-chair, then the rest — alphabetical inside each role. */
    @Test
    void ordersMembersChairThenViceThenAlpha() {
        List<CommitteeDto.Member> unordered = List.of(
                m("Zoe", MembershipRole.MEMBER),
                m("Bob", MembershipRole.MEMBER),
                m("Vera", MembershipRole.VICE_CHAIR),
                m("Carl", MembershipRole.CHAIR),
                m("Ann", MembershipRole.REPRESENTATIVE));

        List<CommitteeDto.Member> sorted = CommitteeService.sortMembers(unordered);

        assertThat(sorted).extracting(CommitteeDto.Member::name)
                .containsExactly("Carl", "Vera", "Ann", "Bob", "Zoe");
    }
}
