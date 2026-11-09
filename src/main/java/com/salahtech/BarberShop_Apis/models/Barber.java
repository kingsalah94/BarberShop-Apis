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
import jakarta.persistence.ManyToOne;
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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
// equals/hashCode basés sur l'id uniquement : évite la récursion infinie
// Barber.user <-> ApplicationUser.barber (StackOverflowError).
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Table(name = "barbers")
public class Barber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String name; // "Ahmed Benali"

    // Relation OneToOne avec ApplicationUser
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true, nullable = false)
    @ToString.Exclude
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

    @ManyToOne
    @JoinColumn(name = "establishment_type_id")
    private EstablishmentType establishmentType;

    // Tenant : le salon (établissement) auquel ce barbier est rattaché.
    // Nullable pour ne pas casser les barbiers existants / solo (migration douce).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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



}
