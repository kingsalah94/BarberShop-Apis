package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/** Exception ponctuelle aux horaires d'un salon (fermeture ou horaires modifiés). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonExceptionDto {
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private boolean closed;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openAt;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeAt;

    private String reason;
}
