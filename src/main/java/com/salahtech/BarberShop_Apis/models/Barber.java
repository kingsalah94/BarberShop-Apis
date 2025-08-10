package com.salahtech.BarberShop_Apis.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private String name;        // "Ahmed Benali"

    // @NotNull
    // @GeneratedValue(strategy = GenerationType.UUID)
    // @Column(name = "user_id", nullable = false, unique = true)
    // @NotNull(message = "User ID cannot be null")
    // @Size(min = 1, message = "User ID must be provided")

    // ID de l'utilisateur, utilisé pour la relation avec ApplicationUser
    private Long barberId;

    // Relation OneToOne avec ApplicationUser
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private ApplicationUser user;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "salon_name", nullable = false)
    private String salonName;
    
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String location;
    
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    
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
    @OneToMany(mappedBy = "barber", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Availability> availabilities = new ArrayList<>();
    
    // Relations avec BarberService et Booking
    @Builder.Default
    @OneToMany(mappedBy = "barber", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BarberService> barberServices = new ArrayList<>();
    
    @Builder.Default
    @OneToMany(mappedBy = "barber", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    @Column(name = "working_hours")
    private String workingHours; // JSON string for working hours

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

     @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
