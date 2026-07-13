package com.politico.party;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "party")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Party {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "short_name", nullable = false, length = 32)
    private String shortName;

    @Column(name = "full_name", nullable = false, length = 256)
    private String fullName;

    @Column(name = "color_hex", length = 16)
    private String colorHex;

    @Column(name = "official_url", columnDefinition = "text")
    private String officialUrl;

    @Column(nullable = false)
    private boolean active;
}
