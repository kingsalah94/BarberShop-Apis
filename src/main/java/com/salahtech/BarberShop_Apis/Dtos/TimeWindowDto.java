package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/** Une plage horaire (ouverture → fermeture). Un jour peut en avoir plusieurs (ex. pause déjeuner). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeWindowDto {

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openAt;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeAt;
}
