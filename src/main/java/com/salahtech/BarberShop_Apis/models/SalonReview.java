package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Avis laissé par un client après une réservation SALON terminée (Phase B).
 * Un avis par réservation.
 */
@Entity
@Table(name = "salon_reviews", indexes = {
        @Index(name = "idx_sreview_salon", columnList = "salon_id"),
        @Index(name = "idx_sreview_booking", columnList = "salon_booking_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_booking_id", nullable = false, unique = true)
    private SalonBooking salonBooking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Note globale 1–5. */
    @Column(nullable = false)
    private Integer overallRating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    /** Tags séparés par des virgules (ex. "Professionnel,Ponctuel"). */
    @Column(columnDefinition = "TEXT")
    private String tags;

    /** Pourboire optionnel. */
    @Column(precision = 10, scale = 2)
    private BigDecimal tipAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
