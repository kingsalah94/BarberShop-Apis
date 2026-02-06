package com.salahtech.BarberShop_Apis.Dtos;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MessageDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderAvatar;

    private Long receiverId;
    private String receiverName;

    private Long bookingId; // Optionnel
    private String content;
    private Boolean isRead;
    private LocalDateTime sentAt;

    // Helper pour savoir si c'est "Moi" qui ai envoyé le message (utile pour le
    // CSS)
    private boolean isMe;
}