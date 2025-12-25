package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberStatsDto {

    // Statistiques générales
    private Long totalBookings;
    private Long confirmedBookings;
    private Long pendingBookings;
    private Long cancelledBookings;
    private Long completedBookings;

    // Statistiques financières
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal weeklyRevenue;
    private BigDecimal dailyRevenue;

    // Statistiques clients
    private Long totalClients;
    private Long newClientsThisMonth;
    private Long returningClients;

    // Statistiques de performance
    private Double averageRating;
    private Long totalReviews;

    // Prochains rendez-vous
    private List<UpcomingBookingDto> upcomingBookings;

    // Statistiques par période
    private List<BookingTrendDto> weeklyTrend;
    private List<BookingTrendDto> monthlyTrend;

    // Services populaires
    private List<PopularServiceDto> popularServices;

    // Disponibilité
    private Integer availableHoursThisWeek;
    private Integer bookedHoursThisWeek;
    private Double occupancyRate; // Pourcentage

}
