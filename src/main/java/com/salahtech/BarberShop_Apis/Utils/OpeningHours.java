package com.salahtech.BarberShop_Apis.Utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.salahtech.BarberShop_Apis.Dtos.TimeWindowDto;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;

/**
 * Plages d'ouverture, y compris celles qui passent minuit (jours de fête, salons de nuit).
 *
 * <p>Règle : une plage dont la fermeture n'est pas après l'ouverture se termine <b>le lendemain</b>
 * (18:00 → 03:00 ; 20:00 → 00:00 = jusqu'à minuit). Garde-fous : fermeture du lendemain au plus tard
 * à {@link #MAX_OVERNIGHT_CLOSE}, et seule la dernière plage d'une journée peut passer minuit.</p>
 */
public final class OpeningHours {

    /** Heure de fermeture maximale pour une plage qui se termine le lendemain. */
    public static final LocalTime MAX_OVERNIGHT_CLOSE = LocalTime.of(8, 0);

    private OpeningHours() {}

    /** Intervalle réel d'ouverture (dates + heures). */
    public record Interval(LocalDateTime start, LocalDateTime end) {
        public boolean contains(LocalDateTime from, LocalDateTime to) {
            return !from.isBefore(start) && !to.isAfter(end);
        }
        public boolean containsInstant(LocalDateTime t) {
            return !t.isBefore(start) && t.isBefore(end);
        }
    }

    /** La plage se termine-t-elle le lendemain ? */
    public static boolean overnight(LocalTime open, LocalTime close) {
        return open != null && close != null && !close.isAfter(open);
    }

    /** Intervalles réels d'une journée « commerciale » {@code day} (une plage de nuit déborde sur J+1). */
    public static List<Interval> intervals(LocalDate day, List<TimeWindowDto> windows) {
        List<Interval> out = new ArrayList<>();
        if (windows == null) return out;
        for (TimeWindowDto w : windows) {
            if (w == null || w.getOpenAt() == null || w.getCloseAt() == null) continue;
            LocalDateTime start = day.atTime(w.getOpenAt());
            LocalDateTime end = (overnight(w.getOpenAt(), w.getCloseAt()) ? day.plusDays(1) : day).atTime(w.getCloseAt());
            out.add(new Interval(start, end));
        }
        out.sort(Comparator.comparing(Interval::start));
        return out;
    }

    /**
     * Valide les plages d'une journée : ouverture ≠ fermeture, fermeture du lendemain ≤ 08:00,
     * une seule plage de nuit et en dernier, aucun chevauchement.
     */
    public static void validate(List<TimeWindowDto> windows, String label) {
        if (windows == null || windows.isEmpty()) return;
        List<TimeWindowDto> sorted = windows.stream()
                .sorted(Comparator.comparing(TimeWindowDto::getOpenAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        LocalDateTime prevEnd = null;
        LocalDate day = LocalDate.of(2000, 1, 3); // date neutre : seules les heures comptent
        for (int i = 0; i < sorted.size(); i++) {
            TimeWindowDto w = sorted.get(i);
            if (w.getOpenAt() == null || w.getCloseAt() == null) {
                throw new BusinessException("Horaires requis (ouverture/fermeture) pour " + label);
            }
            if (w.getOpenAt().equals(w.getCloseAt())) {
                throw new BusinessException("L'ouverture et la fermeture sont identiques pour " + label);
            }
            boolean night = overnight(w.getOpenAt(), w.getCloseAt());
            if (night && w.getCloseAt().isAfter(MAX_OVERNIGHT_CLOSE)) {
                throw new BusinessException("Fermeture le lendemain au plus tard à "
                        + MAX_OVERNIGHT_CLOSE + " pour " + label + " (vérifiez l'heure de fermeture)");
            }
            if (night && i < sorted.size() - 1) {
                throw new BusinessException("Seule la dernière plage peut se terminer après minuit pour " + label);
            }
            Interval iv = intervals(day, List.of(w)).get(0);
            if (prevEnd != null && iv.start().isBefore(prevEnd)) {
                throw new BusinessException("Les plages se chevauchent pour " + label);
            }
            prevEnd = iv.end();
        }
    }
}
