package com.salahtech.BarberShop_Apis.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageDto {
    private Long userId; // L'ID du destinataire (Client ou Barbier)
    private String title; // Titre de la notif
    private String body; // Contenu du message
    private String type; // Ex: "BOOKING_CREATED", "BOOKING_CANCELLED"
    private Long referenceId; // Ex: ID du Booking (pour le clic sur la notif)
}
