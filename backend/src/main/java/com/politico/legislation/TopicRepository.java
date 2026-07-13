package com.politico.legislation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {
    Optional<Topic> findBySourceNameAndEdid(String sourceName, int edid);
}
