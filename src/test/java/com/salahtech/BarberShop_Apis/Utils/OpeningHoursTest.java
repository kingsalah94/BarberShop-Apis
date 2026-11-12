package com.salahtech.BarberShop_Apis.Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.salahtech.BarberShop_Apis.Dtos.TimeWindowDto;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;

class OpeningHoursTest {

    private static TimeWindowDto w(String open, String close) {
        return TimeWindowDto.builder().openAt(LocalTime.parse(open)).closeAt(LocalTime.parse(close)).build();
    }

    @Test
    void plage_de_nuit_se_termine_le_lendemain() {
        LocalDate d = LocalDate.of(2026, 12, 31);
        var iv = OpeningHours.intervals(d, List.of(w("18:00", "03:00"))).get(0);
        assertThat(iv.start()).isEqualTo(d.atTime(18, 0));
        assertThat(iv.end()).isEqualTo(d.plusDays(1).atTime(3, 0));
        assertThat(iv.contains(d.plusDays(1).atTime(1, 0), d.plusDays(1).atTime(1, 30))).isTrue();
    }

    @Test
    void jusqu_a_minuit() {
        LocalDate d = LocalDate.of(2026, 12, 31);
        var iv = OpeningHours.intervals(d, List.of(w("20:00", "00:00"))).get(0);
        assertThat(iv.end()).isEqualTo(d.plusDays(1).atStartOfDay());
    }

    @Test
    void journee_classique_et_pause_valides() {
        OpeningHours.validate(List.of(w("08:30", "13:00"), w("15:00", "21:00")), "LUNDI");
        OpeningHours.validate(List.of(w("09:00", "13:00"), w("15:00", "02:00")), "VENDREDI");
    }

    @Test
    void garde_fous() {
        assertThatThrownBy(() -> OpeningHours.validate(List.of(w("18:00", "11:00")), "X"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("08:00");
        assertThatThrownBy(() -> OpeningHours.validate(List.of(w("22:00", "02:00"), w("23:00", "23:30")), "X"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> OpeningHours.validate(List.of(w("09:00", "13:00"), w("12:00", "18:00")), "X"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("chevauchent");
        assertThatThrownBy(() -> OpeningHours.validate(List.of(w("09:00", "09:00")), "X"))
                .isInstanceOf(BusinessException.class);
    }
}
