package com.salahtech.BarberShop_Apis.Enums;

public enum ServiceCategory {
    HAIRCUT("Coupe"),
    BEARD("Barbe"),
    STYLING("Styling"),
    COLORING("Coloration"),
    SHAVE("Rasage"),
    TRIM("Taillage"),
    PACKAGE("Forfait"),
    MASSAGE("Massage"),
    TREATMENT("Soin"),
    SHAMPOO("Shampoing"),
    WASH("Lavage"),
    OTHER("Autre");

    private final String displayName;

    ServiceCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
