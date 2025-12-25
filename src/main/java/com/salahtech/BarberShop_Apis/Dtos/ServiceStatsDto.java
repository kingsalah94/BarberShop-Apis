package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour les statistiques détaillées d'un service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceStatsDto {

    private Long serviceId;
    private String serviceName;

    // Statistiques globales
    private Long bookingsCount;
    private BigDecimal revenue;
    private Double rating;

    // Dernière réservation
    private LocalDateTime lastBooking;

    // Tendances (optionnel, peut être null)
    private Integer bookingsThisMonth;
    private Integer bookingsLastMonth;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueLastMonth;
}
