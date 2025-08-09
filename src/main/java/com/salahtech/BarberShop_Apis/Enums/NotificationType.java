package com.salahtech.BarberShop_Apis.Enums;


/**
 * Enum représentant les différents types de notifications.
 */
public enum NotificationType {
    BOOKING_CREATED,      // Réservation créée
    BOOKING_UPDATED,      // Réservation modifiée
    BOOKING_CANCELLED,    // Réservation annulée
    PAYMENT_RECEIVED,     // Paiement reçu
    PAYMENT_FAILED,       // Paiement échoué
    REMINDER,             // Rappel (RDV, paiement…)
    SYSTEM_ALERT,         // Alerte système
    PROMOTION,            // Offre promotionnelle
    GENERAL               // Notification générale
, EMAIL, SMS, PUSH
}

