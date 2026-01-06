package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

import com.salahtech.BarberShop_Apis.Enums.PaymentMethod;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingCreateDto {

    @NotNull(message = "Barber ID is required")
    private Long barberId;

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    private Long clientId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    private PaymentMethod paymentMethod;
}
