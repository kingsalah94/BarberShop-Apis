package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // ex: "Taille Barbe", "Coloration"

    private String description;

    @Column(name = "icon")
    private String icon; // Nom de l'icône Ionic (ex: "cut-outline", "color-palette")

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Lien vers le type d'établissement (ex: "Coloration" appartient à
    // "HAIR_SALON_F")
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "establishment_type_id")
    private EstablishmentType establishmentType;
}