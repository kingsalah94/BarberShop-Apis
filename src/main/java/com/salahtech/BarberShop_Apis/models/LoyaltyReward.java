package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Prestation offerte consommée (fidélité) : rattachée à l'encaissement d'une entrée de file. */
@Entity
@Table(name = "loyalty_rewards",
        uniqueConstraints = @UniqueConstraint(name = "uk_loyalty_reward_entry", columnNames = "queue_entry_id"),
        indexes = @Index(name = "idx_loyalty_reward_client_salon", columnList = "client_id, salon_id"))
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyReward {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "queue_entry_id", nullable = false)
    private QueueEntry queueEntry;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;

    @PrePersist
    void onCreate() {
        if (redeemedAt == null) redeemedAt = LocalDateTime.now();
    }
}
