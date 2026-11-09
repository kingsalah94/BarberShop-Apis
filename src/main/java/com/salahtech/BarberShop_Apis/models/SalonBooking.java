package com.salahtech.BarberShop_Apis.models;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Réservation orientée SALON (Phase B) : le client réserve un salon + un service
 * du catalogue ({@link SalonOffering}) sur un créneau. À la confirmation, on
 * peut l'affecter à un poste ({@link Workstation}) / employé. Modèle parallèle
 * au {@link Booking} barber-centric historique pour ne pas le perturber.
 */
@Entity
@Table(name = "salon_bookings", indexes = {
        @Index(name = "idx_sbooking_salon", columnList = "salon_id"),
        @Index(name = "idx_sbooking_client", columnList = "client_id"),
        @Index(name = "idx_sbooking_start", columnList = "start_at"),
        @Index(name = "idx_sbooking_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private SalonOffering offering;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    /** Poste affecté à la confirmation (optionnel). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workstation_id")
    private Workstation assignedWorkstation;

    /** Employé affecté (optionnel). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee assignedEmployee;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
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
