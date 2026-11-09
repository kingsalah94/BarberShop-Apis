package com.salahtech.BarberShop_Apis.models;

import com.salahtech.BarberShop_Apis.Enums.ConversationStatus;
import com.salahtech.BarberShop_Apis.Enums.ConversationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Fil de discussion entre deux participants (client ↔ barbier, support…).
 * Les messages sont rattachés à une conversation.
 */
@Entity
// Table dédiée (et non "conversations") : une table "conversations" héritée d'une
// définition abandonnée existe en base avec des colonnes NOT NULL incompatibles
// (unread_count_client, client_id…). On l'évite proprement via un nom distinct.
@Table(name = "chat_conversations")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConversationType type = ConversationType.CLIENT_BARBER;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant1_id", nullable = false)
    private ApplicationUser participant1;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant2_id", nullable = false)
    private ApplicationUser participant2;

    // Tenant : salon associé (optionnel, ex. conversation client ↔ barbier d'un salon).
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(length = 1000)
    private String lastMessageContent;

    private LocalDateTime lastMessageAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.lastMessageAt == null) this.lastMessageAt = this.createdAt;
        if (this.status == null) this.status = ConversationStatus.ACTIVE;
        if (this.type == null) this.type = ConversationType.CLIENT_BARBER;
    }
}
