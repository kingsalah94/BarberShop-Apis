package com.salahtech.BarberShop_Apis.Web.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.salahtech.BarberShop_Apis.Dtos.SystemStatusDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SettingsService;
import com.salahtech.BarberShop_Apis.models.GlobalSettings;

@RestController
@RequestMapping("/v1/settings")
@RequiredArgsConstructor
// @CrossOrigin(origins = "*") // À décommenter si tu as des soucis CORS en dev
public class SettingsController {

    private final SettingsService service;

    // GET /v1/settings (Public ou Authentifié selon ton choix)
    // Utile pour charger la devise au démarrage de l'app
    @GetMapping
    public ResponseEntity<GlobalSettings> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    // PUT /v1/settings (Admin seulement)
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalSettings> updateSettings(@RequestBody GlobalSettings settings) {
        return ResponseEntity.ok(service.updateSettings(settings));
    }

    // GET /v1/settings/status (Admin seulement)
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemStatusDto> getStatus() {
        return ResponseEntity.ok(service.getSystemStatus());
    }

    // Actions de maintenance
    @PostMapping("/cache/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> clearCache() {
        service.clearCache();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/backup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createBackup() {
        return ResponseEntity.ok().build(); // Mock
    }

    @PostMapping("/security/scan")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> runSecurityScan() {
        return ResponseEntity.ok().build(); // Mock
    }
}
