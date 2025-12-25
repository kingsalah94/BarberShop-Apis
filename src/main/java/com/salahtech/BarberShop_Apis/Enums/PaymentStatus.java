package com.salahtech.BarberShop_Apis.Enums;

public enum PaymentStatus {
    PENDING, // En attente (Le client n'a pas encore payé)
    PAID, // Payé (L'argent est dans la caisse)
    FAILED, // Échec (Carte refusée)
    REFUNDED // Remboursé
}
