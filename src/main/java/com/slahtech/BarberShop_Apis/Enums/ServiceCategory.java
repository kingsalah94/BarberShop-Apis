package com.slahtech.BarberShop_Apis.Enums;

public enum ServiceCategory {
    HAIRCUT("Coupe"),
    BEARD("Barbe"),
    STYLING("Styling"),
    COLORING("Coloration"),
    TREATMENT("Soin"),
    SHAMPOO("Shampoing");
    
    private final String displayName;
    
    ServiceCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
