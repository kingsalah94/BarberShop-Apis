package com.salahtech.BarberShop_Apis.Enums;

public enum QueueStatus {
    WAITING("En attente"),
    NOTIFIED("Notifié"),
    IN_CHAIR("En service"),
    COMPLETED("Terminé"),
    CANCELLED("Annulé"),
    NO_SHOW("Absent");

    private final String displayName;

    QueueStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}