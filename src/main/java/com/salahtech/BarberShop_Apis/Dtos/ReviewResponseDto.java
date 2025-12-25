package com.salahtech.BarberShop_Apis.Dtos;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReviewResponseDto {
    private Long id;
    private String clientName;
    private String clientAvatar;
    private Long barberId;

    private Integer overallRating;
    private Integer qualityRating;
    private Integer receptionRating;
    private Integer valueForMoneyRating;
    private Integer punctualityRating;

    private String comment;
    private List<String> tags;
    private List<String> photos;
    private BigDecimal tipAmount;

    private LocalDateTime createdAt;
    private String timeAgo; // Ex: "Il y a 2 jours" (Calculé ou null)
}