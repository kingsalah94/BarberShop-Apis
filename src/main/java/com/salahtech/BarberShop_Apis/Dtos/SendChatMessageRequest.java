package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendChatMessageRequest {
    @NotNull(message = "La conversation est requise")
    private Long conversationId;

    private String content;

    private MessageType messageType = MessageType.TEXT;

    private Long replyToId;

    /** Optionnel : rattacher le message à un RDV. */
    private Long bookingId;
}
