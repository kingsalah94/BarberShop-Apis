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
        @Index(name = "idx_avail_date", columnList = "date")
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

    /** Heure de début/fin du créneau (obligatoires) */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "note")
    private String note;

    /** Slots calculés/stockés si tu en as l’usage (sinon tu peux les retirer) */
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

        // 2) Hours required and consistent
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Availability invalide: 'startTime' et 'endTime' sont requis.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Availability invalide: 'endTime' doit être après 'startTime'.");
        }

        // 3) Defaults
        if (isAvailable == null) {
            isAvailable = true;
        }

        // 4) Normalize: keep only the active mode field
        if (hasDate) {
            this.dayOfWeek = null;
        } else {
            this.date = null;
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
}
