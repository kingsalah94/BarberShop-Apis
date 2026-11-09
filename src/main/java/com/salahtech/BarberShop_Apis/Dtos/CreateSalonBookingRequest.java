package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Demande de réservation : salon (path) + service + date + heure. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSalonBookingRequest {

    @NotNull
    private Long serviceId;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /** Heure de début "HH:mm" (doit correspondre à un créneau disponible). */
    @NotNull
    private String time;

    private String notes;
}
