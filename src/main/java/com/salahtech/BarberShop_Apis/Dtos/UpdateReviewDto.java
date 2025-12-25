package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for updating an existing review
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReviewDto {

    // Overall rating
    @Min(value = 1, message = "La note doit être entre 1 et 5")
    @Max(value = 5, message = "La note doit être entre 1 et 5")
    private Integer overallRating;

    // Detailed ratings
    @Min(value = 1, message = "La note qualité doit être entre 1 et 5")
    @Max(value = 5, message = "La note qualité doit être entre 1 et 5")
    private Integer qualityRating;

    @Min(value = 1, message = "La note accueil doit être entre 1 et 5")
    @Max(value = 5, message = "La note accueil doit être entre 1 et 5")
    private Integer receptionRating;

    @Min(value = 1, message = "La note rapport qualité/prix doit être entre 1 et 5")
    @Max(value = 5, message = "La note rapport qualité/prix doit être entre 1 et 5")
    private Integer valueForMoneyRating;

    @Min(value = 1, message = "La note ponctualité doit être entre 1 et 5")
    @Max(value = 5, message = "La note ponctualité doit être entre 1 et 5")
    private Integer punctualityRating;

    // Comment
    @Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1000 caractères")
    private String comment;

    // Tags
    @Size(max = 5, message = "Maximum 5 tags autorisés")
    private List<String> tags;

    // Photos
    @Size(max = 5, message = "Maximum 5 photos autorisées")
    private List<String> photos;

    // Tip
    @DecimalMin(value = "0.0", inclusive = false, message = "Le pourboire doit être positif")
    private BigDecimal tipAmount;
}
