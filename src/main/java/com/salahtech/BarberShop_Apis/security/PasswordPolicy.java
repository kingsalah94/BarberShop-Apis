package com.salahtech.BarberShop_Apis.security;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.salahtech.BarberShop_Apis.Exceptions.BadRequestException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SecuritySettingsService;
import com.salahtech.BarberShop_Apis.security.Dtos.SecuritySettingsDto;

import lombok.RequiredArgsConstructor;

/**
 * Politique de mots de passe pilotée par Paramètres › Sécurité (table security_settings).
 * Appliquée aux mots de passe choisis par une personne : inscription, changement,
 * réinitialisation, création de compte par un admin.
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    /** Plancher absolu, même si l'admin saisit moins. */
    static final int HARD_MIN = 6;

    private final SecuritySettingsService settings;

    /** Lève {@link BadRequestException} avec la liste des règles non respectées. */
    public void validate(String password) {
        List<String> missing = violations(password, settings.get());
        if (!missing.isEmpty()) {
            throw new BadRequestException("Mot de passe trop faible : " + String.join(", ", missing) + ".");
        }
    }

    /** Règles non respectées (vide si conforme). Statique pour les tests. */
    static List<String> violations(String password, SecuritySettingsDto s) {
        String pw = password == null ? "" : password;
        List<String> out = new ArrayList<>();
        int min = Math.max(HARD_MIN, s.getPasswordMinLength() == null ? 8 : s.getPasswordMinLength());
        if (pw.length() < min) out.add("au moins " + min + " caractères");
        if (Boolean.TRUE.equals(s.getPasswordRequireUpper()) && pw.chars().noneMatch(Character::isUpperCase)) out.add("une majuscule");
        if (Boolean.TRUE.equals(s.getPasswordRequireLower()) && pw.chars().noneMatch(Character::isLowerCase)) out.add("une minuscule");
        if (Boolean.TRUE.equals(s.getPasswordRequireDigit()) && pw.chars().noneMatch(Character::isDigit)) out.add("un chiffre");
        if (Boolean.TRUE.equals(s.getPasswordRequireSymbol()) && pw.chars().allMatch(Character::isLetterOrDigit)) out.add("un symbole");
        return out;
    }

    /** Vrai si le mot de passe (changé à {@code changedAt}) a dépassé la durée de validité configurée. */
    public boolean isExpired(LocalDateTime changedAt) {
        Integer days = settings.get().getPasswordExpiryDays();
        return isExpired(changedAt, days, LocalDateTime.now());
    }

    static boolean isExpired(LocalDateTime changedAt, Integer days, LocalDateTime now) {
        if (days == null || days <= 0 || changedAt == null) return false;
        return changedAt.plusDays(days).isBefore(now);
    }
}
