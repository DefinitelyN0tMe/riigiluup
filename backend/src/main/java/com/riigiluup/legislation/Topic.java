package com.riigiluup.legislation;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "topic")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Topic {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    @Column(nullable = false)
    private int edid;

    @Column(nullable = false, length = 512)
    private String text;
}
