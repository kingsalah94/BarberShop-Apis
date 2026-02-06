package com.salahtech.BarberShop_Apis.Dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Ajout du Builder pour simplifier le mapping
public class NotificationMessageDto {
    private Long id; // ✅ Ajouté : ID unique de la notif (pour le front)
    private Long userId; // L'ID du destinataire
    private String title; // Titre
    private String body; // Message
    private String type; // "BOOKING", "INFO", "WARNING" (pour la couleur CSS)
    private Long referenceId; // ID du Booking lié
    private boolean read; // ✅ Ajouté : Statut lu/non lu
    private LocalDateTime createdAt; // ✅ Ajouté : Pour afficher "Il y a 2 min"
}
