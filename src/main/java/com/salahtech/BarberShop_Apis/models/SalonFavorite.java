package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Salon mis en favori par un client (modèle salon ; remplace les favoris « barbier »). */
@Entity
@Table(name = "salon_favorites",
        uniqueConstraints = @UniqueConstraint(name = "uk_salon_favorite", columnNames = {"client_id", "salon_id"}),
        indexes = @Index(name = "idx_salon_favorite_client", columnList = "client_id"))
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonFavorite {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
