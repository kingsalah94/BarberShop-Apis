package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.ConversationStatus;
import com.salahtech.BarberShop_Apis.Enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {
    private Long id;
    private ConversationType type;

    private Long participant1Id;
    private String participant1Name;
    private String participant1Avatar;

    private Long participant2Id;
    private String participant2Name;
    private String participant2Avatar;

    // Id du participant côté client (utile pour l'affichage côté front).
    private Long client;
    private Long salonId;

    private ConversationStatus status;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private int unreadCount;
    private LocalDateTime createdAt;
}
