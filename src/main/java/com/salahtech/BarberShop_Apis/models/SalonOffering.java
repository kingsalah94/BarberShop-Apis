package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service proposé au niveau d'un SALON (catalogue salon), indépendant des
 * services barbier historiques ({@link BarberService}). C'est ce catalogue que
 * le client choisit lorsqu'il réserve « le salon + un service » (Phase B).
 */
@Entity
@Table(name = "salon_services", indexes = {
        @Index(name = "idx_salon_offering_salon", columnList = "salon_id")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonOffering {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Durée en minutes (sert au calcul des créneaux). */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /** Image de la prestation (URL servie via /uploads). */
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    /** Catégorie optionnelle (réutilise le référentiel existant). */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
