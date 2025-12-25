package com.salahtech.BarberShop_Apis.Dtos;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequestDto {
    @NotNull(message = "L'ID du barbier est obligatoire")
    private Long barberId;

    @NotNull(message = "L'ID du service est obligatoire")
    private Long serviceId;

    @NotNull(message = "La date de début est obligatoire")
    @Future(message = "Le rendez-vous doit être dans le futur") // Validation automatique
    private LocalDateTime startAt;

    // ...
}
