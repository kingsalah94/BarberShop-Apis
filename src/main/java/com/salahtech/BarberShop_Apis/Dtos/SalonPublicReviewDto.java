package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Avis public d'un salon : jamais de contact client, prénom + initiale seulement. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonPublicReviewDto {
    private Long id;
    private int rating;
    private String comment;
    private String tags;
    private String clientName;
    private String serviceName;
    private LocalDateTime createdAt;
}
