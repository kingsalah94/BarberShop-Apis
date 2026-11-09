package com.salahtech.BarberShop_Apis.models;

import com.salahtech.BarberShop_Apis.Enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
// Table dédiée (et non "messages") : une table "messages" héritée porte une FK
// conversation_id obsolète pointant vers l'ancienne table "conversations". On
// repart proprement sur chat_messages → chat_conversations (tables vides).
@Table(name = "chat_messages")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Fil de discussion auquel appartient le message.
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    // L'expéditeur (peut être un Barbier ou un Client)
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private ApplicationUser sender;

    // Le destinataire
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private ApplicationUser receiver;

    // Optionnel : Lier le message à un RDV spécifique
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // Message auquel celui-ci répond (optionnel).
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private Message replyTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    private LocalDateTime readAt;

    private LocalDateTime editedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
        if (this.isRead == null) this.isRead = false;
        if (this.isDeleted == null) this.isDeleted = false;
        if (this.messageType == null) this.messageType = MessageType.TEXT;
    }
}