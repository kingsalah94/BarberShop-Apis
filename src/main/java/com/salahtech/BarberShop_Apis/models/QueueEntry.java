package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "queue_entries")
public class QueueEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName; // Juste le prénom souvent
    private int numberOfPeople;

    @ManyToOne
    private Salon salon;

    // Si le client veut un employé spécifique
    @ManyToOne
    private Employee preferredEmployee;

    private LocalDateTime checkInTime; // Heure d'arrivée
    private Integer estimatedWaitTime; // Minutes estimées

    @Enumerated(EnumType.STRING)
    private QueueStatus status; // WAITING, IN_CHAIR, COMPLETED, CANCELLED
}

enum QueueStatus {
    WAITING, IN_CHAIR, COMPLETED, CANCELLED
}