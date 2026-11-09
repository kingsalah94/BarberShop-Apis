package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Exception ponctuelle aux horaires d'un salon pour une date précise :
 * fermeture exceptionnelle ({@code closed=true}) ou horaires modifiés
 * ({@code closed=false} + openAt/closeAt). Prioritaire sur {@link SalonHours}.
 */
@Entity
@Table(name = "salon_exceptions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_salon_exception_salon_date", columnNames = {"salon_id", "exc_date"})
}, indexes = {
        @Index(name = "idx_salon_exc_salon", columnList = "salon_id"),
        @Index(name = "idx_salon_exc_date", columnList = "exc_date")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonException {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Column(name = "exc_date", nullable = false)
    private LocalDate date;

    /** Fermé toute la journée : openAt/closeAt ignorés. */
    @Column(name = "closed", nullable = false)
    @Builder.Default
    private boolean closed = true;

    @Column(name = "open_at")
    private LocalTime openAt;

    @Column(name = "close_at")
    private LocalTime closeAt;

    @Column(name = "reason", length = 255)
    private String reason;
}
