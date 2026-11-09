package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Statistiques agrégées des réservations d'un salon sur une période. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonStatsDto {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate from;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate to;

    private long total;
    private long pending;
    private long confirmed;
    private long completed;
    private long cancelled;
    private long noShow;

    /** Chiffre d'affaires estimé (réservations confirmées / réservées / terminées). */
    private BigDecimal revenue;

    private List<NameCount> topServices;
    private List<DayCount> perDay;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameCount {
        private String name;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayCount {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private long count;
    }
}
