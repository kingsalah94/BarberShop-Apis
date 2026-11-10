package com.salahtech.BarberShop_Apis.Enums;

/** Niveaux du référentiel géographique, du plus large au plus précis. */
public enum LocalityType {
    COUNTRY, REGION, CITY, DISTRICT;

    /** Niveau enfant (null pour un quartier). */
    public LocalityType child() {
        return switch (this) {
            case COUNTRY -> REGION;
            case REGION -> CITY;
            case CITY -> DISTRICT;
            case DISTRICT -> null;
        };
    }

    public String label() {
        return switch (this) {
            case COUNTRY -> "Pays";
            case REGION -> "Région";
            case CITY -> "Ville";
            case DISTRICT -> "Quartier";
        };
    }
}
