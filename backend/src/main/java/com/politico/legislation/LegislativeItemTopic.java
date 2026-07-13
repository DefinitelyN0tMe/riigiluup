package com.politico.legislation;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "legislative_item_topic")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegislativeItemTopic {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legislative_item_id")
    private LegislativeItem legislativeItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;
}
