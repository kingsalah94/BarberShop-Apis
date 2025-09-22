package com.salahtech.BarberShop_Apis.Dtos;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileStats {
    // Pour Client
    private Integer totalBookings;
    private Integer upcomingBookings;
    private BigDecimal totalSpent;

    // Pour Barber
    private Integer totalClients;
    private Integer todayBookings;
    private BigDecimal monthlyRevenue;
    private Double averageRating;
}
