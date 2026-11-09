package com.salahtech.BarberShop_Apis.models;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a client's favorite barber
 * Tracks when barber was added to favorites and visit statistics
 */
@Entity
@Table(name = "client_favorites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"client_id", "barber_id"})
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientFavorite {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Column
    private LocalDateTime lastVisit;

    @Column(nullable = false)
    private Integer visitCount = 0;

    @PrePersist
    protected void onCreate() {
        this.addedAt = LocalDateTime.now();
        if (this.visitCount == null) {
            this.visitCount = 0;
        }
    }

    // Helper methods
    public void incrementVisitCount() {
        this.visitCount++;
        this.lastVisit = LocalDateTime.now();
    }
}
