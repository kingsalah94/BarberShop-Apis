package com.salahtech.BarberShop_Apis.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsDto {

    // Stats générales
    private Long totalBookings;
    private Long completedBookings;
    private Long pendingBookings;
    private Long cancelledBookings;
    
    // Stats financières
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal dailyRevenue;
    
    // Stats temporelles
    private Long todayBookings;
    private Long weekBookings;
    private Long monthBookings;
    
    // Dernière mise à jour
    private LocalDateTime lastUpdated;
}
