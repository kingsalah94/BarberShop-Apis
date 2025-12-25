package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ReviewRequestDto {
    @NotNull
    private Long bookingId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer overallRating;

    // Notes détaillées (Optionnelles)
    @Min(1)
    @Max(5)
    private Integer qualityRating;
    @Min(1)
    @Max(5)
    private Integer receptionRating;
    @Min(1)
    @Max(5)
    private Integer valueForMoneyRating;
    @Min(1)
    @Max(5)
    private Integer punctualityRating;

    @NotBlank
    private String comment;

    private List<String> tags; // Le front envoie ["Rapide", "Propre"]
    private List<String> photos; // Le front envoie ["url1", "url2"]

    private BigDecimal tipAmount;
}