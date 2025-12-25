package com.salahtech.BarberShop_Apis.Dtos;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotDto {

    private LocalDateTime startAt; // Changé de startTime à startAt
    private LocalDateTime endAt; // Changé de endTime à endAt
    private boolean available;
    private Long bookingId; // Si le créneau est réservé
    private String reason; // Raison de la réservation ou du blocage

}