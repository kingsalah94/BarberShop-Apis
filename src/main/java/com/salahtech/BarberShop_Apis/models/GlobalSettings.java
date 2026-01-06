package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "global_settings")
public class GlobalSettings {

    @Id
    private Long id = 1L; // Toujours 1 (Singleton)

    // --- Informations App ---
    private String appName = "Barber Shop Pro";
    private String appDescription = "Gestion de salon de coiffure";
    private String contactUrl = "https://monsalon.com";
    private String supportEmail = "admin@monsalon.com";

    // --- Configuration Système ---
    private String timezone = "Africa/Niamey"; // Par défaut pour toi
    private String defaultLanguage = "fr";
    private String dateFormat = "dd/MM/yyyy";

    // 🔥 C'est ici qu'on stocke la devise (ex: "XOF", "EUR", "USD")
    private String currency = "XOF";

    // --- Préférences (Booleans) ---
    private boolean enableNotifications = true;
    private boolean enableAnimations = true;
    private boolean enableAnalytics = false;
    private boolean autoUpdate = true;
    private boolean autoDarkMode = false;
    private boolean autoSave = true;
}