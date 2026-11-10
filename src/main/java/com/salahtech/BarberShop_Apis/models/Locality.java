package com.salahtech.BarberShop_Apis.models;

import java.time.LocalDateTime;

import com.salahtech.BarberShop_Apis.Enums.LocalityType;

import jakarta.persistence.*;
import lombok.*;

/** Nœud du référentiel Pays › Région › Ville › Quartier. Égalité sur l'id seul (pas de récursion Lombok). */
@Entity
@Table(name = "localities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Locality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LocalityType type;

    @Column(nullable = false, length = 120)
    @ToString.Include
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Locality parent;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /** Ajoutée par un salon ou un client : visible tout de suite, à valider par l'admin. */
    @Builder.Default
    @Column(nullable = false)
    private boolean suggested = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
