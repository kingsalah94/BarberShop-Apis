package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Horaire d'ouverture récurrent d'un salon : <b>une ligne = une plage</b>.
 * Un jour peut donc avoir plusieurs lignes (ex. matin + après-midi).
 * Un jour sans ligne (ou une ligne {@code closed=true}) signifie « fermé ».
 * Les exceptions ponctuelles (dates) sont gérées par {@link SalonException}.
 */
@Entity
@Table(name = "salon_hours", indexes = {
        @Index(name = "idx_salon_hours_salon", columnList = "salon_id"),
        @Index(name = "idx_salon_hours_salon_day", columnList = "salon_id,day_of_week")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonHours {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    /** Fermé toute la journée : openAt/closeAt ignorés. */
    @Column(name = "closed", nullable = false)
    @Builder.Default
    private boolean closed = false;

    @Column(name = "open_at")
    private LocalTime openAt;

    @Column(name = "close_at")
    private LocalTime closeAt;
}
