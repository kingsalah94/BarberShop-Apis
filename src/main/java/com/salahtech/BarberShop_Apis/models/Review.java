package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a review left by a client for a barber after a booking
 */
@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    // Ratings (1-5 stars)
    @Column(nullable = false)
    private Integer overallRating;

    private Integer qualityRating;
    private Integer receptionRating;
    private Integer valueForMoneyRating;
    private Integer punctualityRating;

    // Comment
    @Column(columnDefinition = "TEXT")
    private String comment;

    // Tags stored as JSON array: ["Professionnel", "Rapide", "À l'écoute"]
    @Column(columnDefinition = "TEXT")
    private String tags;

    // Photo URLs stored as JSON array: ["url1", "url2"]
    @Column(columnDefinition = "TEXT")
    private String photos;

    @Column(columnDefinition = "TEXT")
    private String barberReply;

    @Column
    private LocalDateTime repliedAt; // Date de la réponse

    // Optional tip given by client
    @Column(precision = 10, scale = 2)
    private BigDecimal tipAmount;

    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // Soft delete
    @Column(nullable = false)
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method to calculate average rating
    public Double getAverageDetailedRating() {
        int count = 0;
        int sum = 0;

        if (qualityRating != null) {
            sum += qualityRating;
            count++;
        }
        if (receptionRating != null) {
            sum += receptionRating;
            count++;
        }
        if (valueForMoneyRating != null) {
            sum += valueForMoneyRating;
            count++;
        }
        if (punctualityRating != null) {
            sum += punctualityRating;
            count++;
        }

        return count > 0 ? (double) sum / count : null;
    }
}
