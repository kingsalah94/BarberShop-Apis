package com.salahtech.BarberShop_Apis.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "DashboardStatsDto", description = "Statistiques agrégées pour le tableau de bord")
public class DashboardStatsDto {

    // Stats générales
    @Schema(example = "120")
    private long totalBookings;

    @Schema(example = "86")
    private long completedBookings;

    @Schema(example = "22")
    private long pendingBookings;

    @Schema(example = "12")
    private long cancelledBookings;

    // Stats financières (toujours non null pour éviter les NPE côté front)
    @Builder.Default
    @Schema(example = "15420.50")
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Builder.Default
    @Schema(example = "1240.00")
    private BigDecimal monthlyRevenue = BigDecimal.ZERO;

    @Builder.Default
    @Schema(example = "180.00")
    private BigDecimal dailyRevenue = BigDecimal.ZERO;

    // Stats temporelles
    @Schema(example = "5")
    private long todayBookings;

    @Schema(example = "28")
    private long weekBookings;

    @Schema(example = "110")
    private long monthBookings;

    // Dernière mise à jour
    @Schema(example = "2025-09-17T19:41:26")
    private LocalDateTime lastUpdated;

    /** Optionnel : s'assurer que les montants ne sont jamais null après mapping */
    public DashboardStatsDto normalize() {
        if (totalRevenue == null)
            totalRevenue = BigDecimal.ZERO;
        if (monthlyRevenue == null)
            monthlyRevenue = BigDecimal.ZERO;
        if (dailyRevenue == null)
            dailyRevenue = BigDecimal.ZERO;
        return this;
    }
}
