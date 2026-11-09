package com.salahtech.BarberShop_Apis.Enums;

public enum EmployeeRole {
    BARBER("Barbier"),
    MANAGER("Manager"),
    RECEPTIONIST("Réceptionniste"),
    ASSISTANT("Assistant"),
    APPRENTICE("Apprenti");

    private final String displayName;

    EmployeeRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}