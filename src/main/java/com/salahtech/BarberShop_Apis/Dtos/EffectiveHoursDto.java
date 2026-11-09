package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Horaires effectifs d'un salon pour une date donnée, après résolution
 * (exception ponctuelle prioritaire, sinon semaine type). Servira au calcul
 * des créneaux de réservation et au catalogue client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveHoursDto {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private boolean closed;

    /** Plages effectives du jour (multi-créneaux). Source de vérité pour les créneaux. */
    private List<TimeWindowDto> windows;

    /** @deprecated conservé pour rétro-compat (= 1ʳᵉ plage). Préférez {@link #windows}. */
    @Deprecated
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openAt;

    /** @deprecated conservé pour rétro-compat (= 1ʳᵉ plage). Préférez {@link #windows}. */
    @Deprecated
    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeAt;

    /** Origine de la résolution : "EXCEPTION" ou "WEEKLY". */
    private String source;

    /** Raison (si fermeture/exception). */
    private String reason;
}
