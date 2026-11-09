package com.salahtech.BarberShop_Apis.models;

import com.salahtech.BarberShop_Apis.Enums.PaymentMethod;
import com.salahtech.BarberShop_Apis.Enums.PaymentStatus;
import com.salahtech.BarberShop_Apis.Enums.QueueStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "queue_entries", indexes = {
        @Index(name = "idx_queue_salon_status", columnList = "salon_id,status"),
        @Index(name = "idx_queue_check_in", columnList = "check_in_time")
})
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

     // Juste le prénom souvent
    @Column(nullable = false)
    private String customerName;

    private String customerPhone;

    @Column(nullable = false)
    @Builder.Default
    private int numberOfPeople = 1;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    // Si le client veut un employé spécifique
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_employee_id")
    private Employee preferredEmployee;


    // Minutes estimées
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private Employee assignedEmployee;

    // Modèle barber-centric : barbier souhaité / barbier qui prend en charge.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_barber_id")
    private Barber preferredBarber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_barber_id")
    private Barber assignedBarber;

    // Lien vers le RDV d'origine (si l'entrée vient d'un check-in de Booking).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workstation_id")
    private Workstation workstation;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "queue_entry_services",
            joinColumns = @JoinColumn(name = "queue_entry_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    private List<BarberService> requestedServices = new ArrayList<>();

    @Column(name = "ticket_number", nullable = false)
    private String ticketNumber;

    // Heure d'arrivée
    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // Minutes estimées
    @Column(name = "estimated_wait_time")
    private Integer estimatedWaitTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QueueStatus status = QueueStatus.WAITING; // WAITING, IN_CHAIR, COMPLETED, CANCELLED

    @Column(length = 500)
    private String notes;

    // ----- Paiement (encaissé à la fin du service) -----
    // Nullable au niveau DB : évite l'échec d'ALTER (ddl-auto=update) sur une table
    // déjà remplie. Les nouvelles lignes reçoivent PENDING via @Builder.Default.
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    /** Réservation salon d'origine (flux salon-centric) — pour idempotence + prix. */
    @Column(name = "salon_booking_id")
    private Long salonBookingId;

    /** Prix attendu (défini par le service réservé) — sert de montant à encaisser. */
    @Column(name = "expected_price", precision = 10, scale = 2)
    private BigDecimal expectedPrice;

    @PrePersist
    protected void onCreate() {
        if (checkInTime == null) {
            checkInTime = LocalDateTime.now();
        }
    }
}