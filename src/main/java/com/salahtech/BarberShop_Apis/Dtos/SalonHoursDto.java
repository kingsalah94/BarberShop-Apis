package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/** Horaire d'un jour de la semaine type d'un salon (peut comporter plusieurs plages). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonHoursDto {
    private DayOfWeek dayOfWeek;
    private boolean closed;

    /** Plages du jour (multi-créneaux, ex. matin + après-midi). Source de vérité. */
    private List<TimeWindowDto> windows;

    /** @deprecated conservé pour rétro-compat (= 1ʳᵉ plage). Préférez {@link #windows}. */
    @Deprecated
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openAt;

    /** @deprecated conservé pour rétro-compat (= 1ʳᵉ plage). Préférez {@link #windows}. */
    @Deprecated
    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeAt;
}
