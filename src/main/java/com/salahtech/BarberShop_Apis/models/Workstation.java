package com.salahtech.BarberShop_Apis.models;

import com.salahtech.BarberShop_Apis.Enums.StationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "workstations")
public class Workstation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

 // "Fauteuil 1", "Bac à shampoing"
    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    // FREE, OCCUPIED, CLEANING
    @Column(nullable = false)
    @Builder.Default
    private StationStatus status = StationStatus.FREE;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_employee_id")
    private Employee currentEmployee;

    // Barbier affecté au poste (modèle barber-centric — déprécie currentEmployee
    // pour ce flux). La file d'attente assigne le client à ce barbier.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_barber_id")
    private Barber currentBarber;
    
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
}