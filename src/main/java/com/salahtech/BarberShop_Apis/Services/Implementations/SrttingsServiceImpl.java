package com.salahtech.BarberShop_Apis.Services.Implementations;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Objects;

import com.salahtech.BarberShop_Apis.Dtos.SystemStatusDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SettingsService;
import com.salahtech.BarberShop_Apis.models.GlobalSettings;
import com.salahtech.BarberShop_Apis.reppsitories.GlobalSettingsRepository;

@Service
@RequiredArgsConstructor
public class SrttingsServiceImpl implements SettingsService {

    private final GlobalSettingsRepository repository;
    private final CacheManager cacheManager; // Inject CacheManager

    // Récupérer la config (ou créer par défaut)
    @Override
    public GlobalSettings getSettings() {
        return repository.findById(1L).orElseGet(() -> {
            GlobalSettings defaultSettings = new GlobalSettings();
            defaultSettings.setId(1L);
            return repository.save(defaultSettings);
        });
    }

    // Mettre à jour
    @Override
    public GlobalSettings updateSettings(GlobalSettings newSettings) {
        newSettings.setId(1L); // Sécurité : on force l'ID 1
        return repository.save(newSettings);
    }

    // Calculer les stats réelles du serveur (Uptime, Disque)
    @Override
    public SystemStatusDto getSystemStatus() {
        // Calcul Uptime
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration d = Duration.ofMillis(uptimeMillis);
        String uptime = String.format("%dj %dh %dm", d.toDays(), d.toHoursPart(), d.toMinutesPart());

        // Calcul Stockage Disque
        File root = new File(".");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        int storagePercent = (totalSpace > 0) ? (int) ((double) usedSpace / totalSpace * 100) : 0;

        return SystemStatusDto.builder()
                .version("1.0.0")
                .uptime(uptime)
                .storageUsed(storagePercent)
                .performance("Optimal")
                .cacheSize("12 MB") // Valeur simulée pour l'exemple
                .lastBackup("Hier 03:00") // Valeur simulée
                .build();
    }

    @Override
    public void clearCache() {
        // On récupère tous les noms de cache (ex: "barbers", "services", "settings")
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (Objects.nonNull(cache)) {
                cache.clear(); // Vide le cache spécifique
            }
        });

        System.out.println("✅ Tous les caches ont été vidés via l'interface Admin.");
    }
}
