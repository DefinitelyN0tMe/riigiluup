package com.politico.statistics;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberParticipationCacheRepository
        extends JpaRepository<MemberParticipationCache, String> {
}
