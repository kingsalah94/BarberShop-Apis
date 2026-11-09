package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message tel qu'attendu par le front de messagerie (clé `createdAt`,
 * `messageType`, booléens `isRead`/`isDeleted`).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageDto {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private MessageType messageType;
    private Long replyToId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime editedAt;
    // Boxed Boolean → Lombok getIsRead()/getIsDeleted() → clés JSON "isRead"/"isDeleted".
    private Boolean isRead;
    private Boolean isDeleted;
}
