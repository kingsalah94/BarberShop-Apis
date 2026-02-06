package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "workstations")
public class Workstation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // "Fauteuil 1", "Bac à shampoing"

    @Enumerated(EnumType.STRING)
    private StationStatus status; // FREE, OCCUPIED, CLEANING

    @ManyToOne
    private Salon salon;
}

enum StationStatus {
    FREE, OCCUPIED, CLEANING, OUT_OF_SERVICE
}