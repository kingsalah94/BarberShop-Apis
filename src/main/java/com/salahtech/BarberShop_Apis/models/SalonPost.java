package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Publication / réalisation d'un SALON (portfolio) : une photo (coupe, modèle,
 * client coiffé) + légende. Affichée sur la fiche salon et dans le fil « À la une ».
 * Modèle salon-centric, parallèle au {@link Post} barbier historique.
 */
@Entity
@Table(name = "salon_posts", indexes = {
        @Index(name = "idx_salonpost_salon", columnList = "salon_id"),
        @Index(name = "idx_salonpost_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
