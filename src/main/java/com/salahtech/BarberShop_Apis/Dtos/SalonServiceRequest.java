package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Création / mise à jour d'un service de salon. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonServiceRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;

    @NotNull
    @Min(1)
    private Integer durationMinutes;

    /** URL de l'image (déjà uploadée). */
    private String imageUrl;

    /** Catégorie optionnelle (référentiel existant). */
    private Long categoryId;

    private Boolean active;
}
