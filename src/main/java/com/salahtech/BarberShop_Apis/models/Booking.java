package com.salahtech.BarberShop_Apis.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.auto.value.AutoValue.Builder;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bookings")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private ApplicationUser user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private BarberService barberService;
    
    @NotNull
    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;
    
    @NotNull
    @DecimalMin("0.0")
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endAt;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    private LocalTime bookingTime;

    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Retourne l'heure de début du créneau de réservation
     * @return LocalDateTime
     */



    public LocalDateTime getStartTime() {
        return bookingDate;
    }
    
    public LocalDateTime getEndTime() {
        return bookingDate.plusMinutes(barberService.getDuration());
    }
    
    
    
}   