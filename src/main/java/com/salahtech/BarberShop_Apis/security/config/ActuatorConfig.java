package com.salahtech.BarberShop_Apis.security.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {
    
    @Bean
    public HealthIndicator authServiceHealthIndicator() {
        return () -> {
            // Vérifier la santé du service d'authentification
            try {
                // Vérifications basiques
                return Health.up()
                        .withDetail("service", "auth-service")
                        .withDetail("status", "operational")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("service", "auth-service")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}
