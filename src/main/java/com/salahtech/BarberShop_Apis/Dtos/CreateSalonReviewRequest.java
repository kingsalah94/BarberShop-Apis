package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Avis client sur une réservation salon terminée. */
@Data
public class CreateSalonReviewRequest {

    @NotNull(message = "La note est requise")
    @Min(value = 1, message = "Note minimale : 1")
    @Max(value = 5, message = "Note maximale : 5")
    private Integer overallRating;

    private String comment;

    private List<String> tags;

    private BigDecimal tipAmount;
}
