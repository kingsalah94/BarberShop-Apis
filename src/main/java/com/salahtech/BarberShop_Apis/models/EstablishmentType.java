package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "establishment_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstablishmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // ex: "BARBER", "HAIR_SALON_F", "NAIL_BAR"

    @Column(nullable = false)
    private String name; // ex: "Barber Shop", "Salon de Coiffure Femme"

    private String description;

    @Column(name = "default_image_url")
    private String defaultImageUrl;
}