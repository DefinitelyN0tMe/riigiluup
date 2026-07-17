package com.riigiluup.initiative;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InitiativeCommitteeLinkRepository
        extends JpaRepository<InitiativeCommitteeLink, InitiativeCommitteeLink.Key> {

    List<InitiativeCommitteeLink> findByInitiativeId(Long initiativeId);

    void deleteByInitiativeId(Long initiativeId);
}
