package com.salahtech.BarberShop_Apis.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriétés de configuration pour le stockage des fichiers
 */
@Configuration
@ConfigurationProperties(prefix = "file")
@Data
public class FileStorageProperties {

    /**
     * Répertoire où les fichiers uploadés seront stockés
     */
    private String uploadDir = "uploads/user";
}