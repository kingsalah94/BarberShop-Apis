package com.salahtech.BarberShop_Apis.Dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularServiceDto {
    private Long serviceId;
    private String serviceName;
    private Long bookingCount;
    private BigDecimal totalRevenue;
    private Double averageRating;
}
