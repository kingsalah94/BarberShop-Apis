package com.salahtech.BarberShop_Apis.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Table(name = "barbers")
public class Barber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // "Ahmed Benali"

    // Relation OneToOne avec ApplicationUser
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true, nullable = false)
    private ApplicationUser user;

    @NotBlank
    @Size(max = 100)
    @Column(name = "salon_name", nullable = false)
    private String salonName;

    // Professional information
    @Column(length = 1000)
    private String bio;

    @Column
    private String phoneNumber;

    @Column
    private String address;

    @Column
    private String city;

    @Column
    private String postalCode;

    @Column
    private Integer experienceYears;

    @Column(length = 500)
    private String languages;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String location;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    private Double distance; // La distance en km par rapport au client

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    @Column(precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "reviews_count")
    @Builder.Default
    private Integer reviewsCount = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String specialties; // JSON string

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "price_from", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceFrom;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Nouvelle relation inverse avec Availability
    @Builder.Default
    @OneToMany(mappedBy = "barber", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    private List<Availability> availabilities = new ArrayList<>();

    // Relations avec BarberService et Booking
    @Builder.Default
    @OneToMany(mappedBy = "barber", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    private List<BarberService> barberServices = new ArrayList<>();

    @OneToMany(mappedBy = "barber", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    // ⭐ NOUVELLE RELATION - REVIEWS
    @OneToMany(mappedBy = "barber", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @Column(name = "working_hours")
    private String workingHours; // JSON string for working hours

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    // Status
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    private String profileImageUrl; // URL de l'image de profil du barbier

    private Integer experience; // Années d'expérience du barbier

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (this.rating == null) {
            this.rating = BigDecimal.ZERO;
        }
        if (this.reviewsCount == null) {
            this.reviewsCount = 0;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isVerified == null) {
            this.isVerified = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Update average rating based on all reviews
     * Should be called after a review is added, updated, or deleted
     */
    public void updateAverageRating() {
        List<Review> activeReviews = reviews.stream()
                .filter(r -> !r.getIsDeleted())
                .toList();

        if (activeReviews.isEmpty()) {
            this.rating = BigDecimal.ZERO;
            this.reviewsCount = 0;
        } else {
            double averageRating = activeReviews.stream()
                    .mapToInt(Review::getOverallRating)
                    .average()
                    .orElse(0.0);

            this.rating = BigDecimal.valueOf(averageRating)
                    .setScale(2, RoundingMode.HALF_UP);
            this.reviewsCount = activeReviews.size();
        }
    }

    /**
     * Get count of completed bookings
     */
    public long getCompletedBookingsCount() {
        return bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .count();
    }

    /**
     * Get rating breakdown for each category
     */
    public RatingBreakdown getRatingBreakdown() {
        List<Review> activeReviews = reviews.stream()
                .filter(r -> !r.getIsDeleted())
                .toList();

        if (activeReviews.isEmpty()) {
            return new RatingBreakdown(0.0, 0.0, 0.0, 0.0);
        }

        double quality = activeReviews.stream()
                .filter(r -> r.getQualityRating() != null)
                .mapToInt(Review::getQualityRating)
                .average()
                .orElse(0.0);

        double reception = activeReviews.stream()
                .filter(r -> r.getReceptionRating() != null)
                .mapToInt(Review::getReceptionRating)
                .average()
                .orElse(0.0);

        double value = activeReviews.stream()
                .filter(r -> r.getValueForMoneyRating() != null)
                .mapToInt(Review::getValueForMoneyRating)
                .average()
                .orElse(0.0);

        double punctuality = activeReviews.stream()
                .filter(r -> r.getPunctualityRating() != null)
                .mapToInt(Review::getPunctualityRating)
                .average()
                .orElse(0.0);

        return new RatingBreakdown(quality, reception, value, punctuality);
    }

    // Inner class for rating breakdown
    @Data
    @AllArgsConstructor
    public static class RatingBreakdown {
        private Double quality;
        private Double reception;
        private Double valueForMoney;
        private Double punctuality;
    }

}
