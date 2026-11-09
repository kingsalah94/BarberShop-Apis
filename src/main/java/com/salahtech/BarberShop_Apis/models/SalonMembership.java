package com.salahtech.BarberShop_Apis.models;

import com.salahtech.BarberShop_Apis.Enums.SalonRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Appartenance d'un utilisateur à un salon avec un rôle d'accès
 * (OWNER / MANAGER / RECEPTIONIST). C'est l'autorité multi-tenant :
 * un sous-compte n'a de droits que sur les salons où il a un membership.
 */
@Entity
@Table(
    name = "salon_memberships",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "salon_id"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private ApplicationUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalonRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
