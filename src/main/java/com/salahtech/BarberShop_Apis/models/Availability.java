package com.salahtech.BarberShop_Apis.models;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "availabilities", indexes = {
        @Index(name = "idx_avail_barber", columnList = "barber_id"),
        @Index(name = "idx_avail_dayofweek", columnList = "day_of_week"),
        @Index(name = "idx_avail_date", columnList = "date"),
        @Index(name = "idx_avail_exception_type", columnList = "exception_type") // NOUVEAU
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Plusieurs disponibilités pour un seul barber */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    /** Mode 1 : créneau ponctuel (date spécifique) */
    @Column(name = "date")
    private LocalDate date;

    /** Mode 2 : créneau récurrent (jour de semaine) */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    /** Heure de début/fin du créneau (optionnels pour exceptions CLOSED) */
    @Column(name = "start_at")
    private LocalTime startAt;

    @Column(name = "end_at")
    private LocalTime endAt;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "note")
    private String note;

    // ========== NOUVEAUX CHAMPS POUR LES EXCEPTIONS ==========

    /**
     * Type d'exception : CLOSED (fermé toute la journée) ou CUSTOM (horaires
     * modifiés)
     * Utilisé uniquement quand date != null (exception ponctuelle)
     */
    @Column(name = "exception_type", length = 20)
    private String exceptionType;

    /**
     * Raison de l'exception (congé, formation, événement spécial, etc.)
     * Utilisé uniquement quand date != null (exception ponctuelle)
     */
    @Column(name = "reason", length = 255)
    private String reason;

    // ========== FIN NOUVEAUX CHAMPS ==========

    /** Slots calculés/stockés si tu en as l'usage (sinon tu peux les retirer) */
    @ElementCollection
    @CollectionTable(name = "availability_available_slots", joinColumns = @JoinColumn(name = "availability_id"))
    @Column(name = "slot")
    private List<LocalTime> availableSlots;

    @ElementCollection
    @CollectionTable(name = "availability_booked_slots", joinColumns = @JoinColumn(name = "availability_id"))
    @Column(name = "slot")
    private List<LocalTime> bookedSlots;

    /** -------- Validation & normalisation -------- */

    @PrePersist
    @PreUpdate
    private void validateAndNormalize() {
        // 1) Exactly one mode must be set: date XOR dayOfWeek
        boolean hasDate = this.date != null;
        boolean hasDOW = this.dayOfWeek != null;
        if (hasDate == hasDOW) { // both true OR both false
            throw new IllegalArgumentException(
                    "Availability invalide: définir soit 'date' (ponctuel), soit 'dayOfWeek' (récurrent), mais pas les deux.");
        }

        // 2) Validation spécifique pour les exceptions
        if (hasDate) {
            // C'est une exception ponctuelle
            if (exceptionType != null) {
                // Valider le type d'exception
                if (!"CLOSED".equalsIgnoreCase(exceptionType) && !"CUSTOM".equalsIgnoreCase(exceptionType)) {
                    throw new IllegalArgumentException(
                            "Availability invalide: 'exceptionType' doit être 'CLOSED' ou 'CUSTOM'.");
                }

                // Pour CLOSED : pas besoin de startTime/endTime, isAvailable doit être false
                if ("CLOSED".equalsIgnoreCase(exceptionType)) {
                    this.isAvailable = false;
                    // startTime et endTime peuvent être null pour CLOSED
                }
                // Pour CUSTOM : startTime/endTime requis, isAvailable doit être true
                else if ("CUSTOM".equalsIgnoreCase(exceptionType)) {
                    if (startAt == null || endAt == null) {
                        throw new IllegalArgumentException(
                                "Availability invalide: pour exceptionType='CUSTOM', 'startTime' et 'endTime' sont requis.");
                    }
                    this.isAvailable = true;
                }
            } else {
                // Exception ponctuelle sans type défini : valider les heures normalement
                if (startAt == null || endAt == null) {
                    throw new IllegalArgumentException(
                            "Availability invalide: 'startTime' et 'endTime' sont requis.");
                }
            }
        } else {
            // Créneau récurrent : les heures sont toujours requises
            if (startAt == null || endAt == null) {
                throw new IllegalArgumentException(
                        "Availability invalide: 'startTime' et 'endTime' sont requis pour les créneaux récurrents.");
            }
            // Les champs exception ne doivent pas être utilisés pour les créneaux
            // récurrents
            if (exceptionType != null || reason != null) {
                throw new IllegalArgumentException(
                        "Availability invalide: 'exceptionType' et 'reason' sont réservés aux exceptions ponctuelles (date != null).");
            }
        }

        // 3) Validation des heures si présentes
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "Availability invalide: 'endTime' doit être après 'startTime'.");
        }

        // 4) Defaults
        if (isAvailable == null) {
            isAvailable = true;
        }

        // 5) Normalize: keep only the active mode field
        if (hasDate) {
            this.dayOfWeek = null;
        } else {
            this.date = null;
            // Nettoyer les champs exception pour les créneaux récurrents
            this.exceptionType = null;
            this.reason = null;
        }
    }

    /** -------- Helpers -------- */

    /** true si créneau récurrent (par jour de semaine) */
    public boolean isRecurring() {
        return dayOfWeek != null && date == null;
    }

    /** true si créneau ponctuel (date spécifique) */
    public boolean isOneOff() {
        return date != null && dayOfWeek == null;
    }

    /**
     * true si c'est une exception (créneau ponctuel avec type d'exception défini)
     */
    public boolean isException() {
        return isOneOff() && exceptionType != null;
    }

    /** true si exception de type CLOSED (fermé toute la journée) */
    public boolean isClosedException() {
        return isException() && "CLOSED".equalsIgnoreCase(exceptionType);
    }

    /** true si exception de type CUSTOM (horaires modifiés) */
    public boolean isCustomException() {
        return isException() && "CUSTOM".equalsIgnoreCase(exceptionType);
    }
}