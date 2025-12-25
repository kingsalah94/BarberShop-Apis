package com.salahtech.BarberShop_Apis.Dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingBookingDto {
    private Long bookingId;
    private String clientName;
    private String serviceName;
    private LocalDateTime appointmentDate;
    private String status;
    private BigDecimal price;
}
