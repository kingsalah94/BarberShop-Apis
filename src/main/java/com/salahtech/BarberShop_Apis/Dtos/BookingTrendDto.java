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
public class BookingTrendDto {
    private String period; // "2024-01-01" ou "Week 1"
    private Long bookingCount;
    private BigDecimal revenue;
}
