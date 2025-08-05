package com.salahtech.BarberShop_Apis.Enums;

public enum BookingStatus {
    PENDING("En attente"),
    CONFIRMED("Confirmé"),
    COMPLETED("Terminé"),
    CANCELLED("Annulé"),
    NO_SHOW("Absent");
    
    private final String displayName;
    
    BookingStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
