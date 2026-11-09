package com.salahtech.BarberShop_Apis.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Supprime la contrainte UNIQUE (salon_id, day_of_week) de {@code salon_hours}
 * pour autoriser <b>plusieurs plages par jour</b> (multi-créneaux, ex. pause déjeuner).
 *
 * <p>{@code ddl-auto=update} ne supprime jamais une contrainte existante → on le fait ici,
 * de façon idempotente ({@code DROP … IF EXISTS}). L'ordre/validation des plages est
 * assuré au niveau applicatif ({@code SalonHoursServiceImpl}).</p>
 */
@Slf4j
@Component
@Order(0) // avant les seeders / backfills
@RequiredArgsConstructor
public class SalonHoursConstraintMigration implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("ALTER TABLE salon_hours DROP CONSTRAINT IF EXISTS uk_salon_hours_salon_day");
            log.info("✅ Contrainte UNIQUE uk_salon_hours_salon_day supprimée si présente (multi-plages activé).");
        } catch (Exception e) {
            log.warn("Suppression de uk_salon_hours_salon_day ignorée : {}", e.getMessage());
        }
    }
}
