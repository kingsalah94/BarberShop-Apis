package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les créneaux horaires des exceptions
 * Utilisé pour définir les horaires modifiés lors d'exceptions CUSTOM
 * Format simple avec String "HH:mm" (ex: "09:00", "18:00")
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionTimeSlotDto {

    /**
     * Heure de début au format "HH:mm"
     * Exemple: "09:00", "14:30"
     */
    private String startAt;

    /**
     * Heure de fin au format "HH:mm"
     * Exemple: "18:00", "17:30"
     */
    private String endAt;
}