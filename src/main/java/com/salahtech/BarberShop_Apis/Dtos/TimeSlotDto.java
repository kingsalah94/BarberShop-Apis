package com.salahtech.BarberShop_Apis.Dtos;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSlotDto {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean available;
    private Long bookingId; // Si le créneau est réservé
    private String reason; // Raison de la réservation ou du blocage
}
