package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "salons")
public class Salon {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // "Golden Scissor"
    private String address;
    private Double latitude;
    private Double longitude;
    private String phoneNumber;

    /** Localité la plus précise connue (quartier, sinon ville…) — référentiel Pays › Région › Ville › Quartier. */
    @ToString.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locality_id")
    private Locality locality;

    /** Logo du salon (URL) — branding. */
    private String logoUrl;
    /** Photo de couverture (URL) — branding. */
    private String coverUrl;

    // Le Gérant (Propriétaire du compte)
    @ToString.Exclude
    @OneToOne
    private ApplicationUser owner;

    // Type de salon (Définit les catégories accessibles)
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "establishment_type_id")
    private EstablishmentType type;

    // Liste des postes de travail
    @ToString.Exclude
    @OneToMany(mappedBy = "salon")
    private List<Workstation> workstations;
}