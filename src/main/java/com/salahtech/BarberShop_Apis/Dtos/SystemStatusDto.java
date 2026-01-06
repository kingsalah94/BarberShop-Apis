package com.salahtech.BarberShop_Apis.Dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemStatusDto {
    private String version;
    private String uptime;
    private int storageUsed; // Pourcentage (0-100)
    private String performance; // "Optimal", "Lent", etc.
    private String cacheSize;
    private String lastBackup;
}
