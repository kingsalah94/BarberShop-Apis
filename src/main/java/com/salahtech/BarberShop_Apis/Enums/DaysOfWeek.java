package com.salahtech.BarberShop_Apis.Enums;

public enum DaysOfWeek {
    MONDAY("Lundi"),
    TUESDAY("Mardi"),
    WEDNESDAY("Mercredi"),
    THURSDAY("Jeudi"),
    FRIDAY("Vendredi"),
    SATURDAY("Samedi"),
    SUNDAY("Dimanche");

    private final String displayName;

    DaysOfWeek(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Méthode pour convertir de java.time.DayOfWeek
    public static DaysOfWeek fromJavaTime(java.time.DayOfWeek javaDayOfWeek) {
        return switch (javaDayOfWeek) {
            case MONDAY -> MONDAY;
            case TUESDAY -> TUESDAY;
            case WEDNESDAY -> WEDNESDAY;
            case THURSDAY -> THURSDAY;
            case FRIDAY -> FRIDAY;
            case SATURDAY -> SATURDAY;
            case SUNDAY -> SUNDAY;
        };
    }

    // Méthode pour convertir vers java.time.DayOfWeek
    public java.time.DayOfWeek toJavaTime() {
        return switch (this) {
            case MONDAY -> java.time.DayOfWeek.MONDAY;
            case TUESDAY -> java.time.DayOfWeek.TUESDAY;
            case WEDNESDAY -> java.time.DayOfWeek.WEDNESDAY;
            case THURSDAY -> java.time.DayOfWeek.THURSDAY;
            case FRIDAY -> java.time.DayOfWeek.FRIDAY;
            case SATURDAY -> java.time.DayOfWeek.SATURDAY;
            case SUNDAY -> java.time.DayOfWeek.SUNDAY;
        };
    }
}