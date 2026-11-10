package com.salahtech.BarberShop_Apis.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.salahtech.BarberShop_Apis.security.Dtos.SecuritySettingsDto;

class PasswordPolicyTest {

    private SecuritySettingsDto rules(int min, boolean up, boolean low, boolean digit, boolean sym) {
        return SecuritySettingsDto.builder().passwordMinLength(min).passwordRequireUpper(up)
                .passwordRequireLower(low).passwordRequireDigit(digit).passwordRequireSymbol(sym).build();
    }

    @Test
    void conforme_aux_regles_par_defaut() {
        assertThat(PasswordPolicy.violations("Client@123", rules(8, true, true, true, false))).isEmpty();
    }

    @Test
    void liste_chaque_regle_manquante() {
        assertThat(PasswordPolicy.violations("abc", rules(8, true, true, true, true)))
                .containsExactly("au moins 8 caractères", "une majuscule", "un chiffre", "un symbole");
    }

    @Test
    void plancher_absolu_meme_si_reglage_trop_bas() {
        assertThat(PasswordPolicy.violations("Ab1", rules(2, false, false, false, false)))
                .containsExactly("au moins 6 caractères");
    }

    @Test
    void symbole_exige() {
        assertThat(PasswordPolicy.violations("Abcdefg1", rules(8, true, true, true, true))).containsExactly("un symbole");
        assertThat(PasswordPolicy.violations("Abcdef1!", rules(8, true, true, true, true))).isEmpty();
    }

    @Test
    void expiration() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 10, 12, 0);
        assertThat(PasswordPolicy.isExpired(now.minusDays(91), 90, now)).isTrue();
        assertThat(PasswordPolicy.isExpired(now.minusDays(10), 90, now)).isFalse();
        assertThat(PasswordPolicy.isExpired(now.minusDays(500), 0, now)).isFalse();
        assertThat(PasswordPolicy.isExpired(null, 90, now)).isFalse();
    }
}
