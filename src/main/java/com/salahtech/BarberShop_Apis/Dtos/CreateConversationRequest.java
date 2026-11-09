package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.ConversationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {
    @NotNull(message = "Le participant est requis")
    private Long participantId;

    private ConversationType type = ConversationType.CLIENT_BARBER;

    private Long salonId;

    /** Optionnel : premier message envoyé à la création. */
    private String initialMessage;
}
