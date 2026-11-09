package com.salahtech.BarberShop_Apis.Enums;

public enum StationStatus {
    FREE("Libre"),
    OCCUPIED("Occupé"),
    CLEANING("En nettoyage"),
    OUT_OF_SERVICE("Hors service");

    private final String displayName;

    StationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}