package com.salahtech.BarberShop_Apis.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Supprime les contraintes CHECK d'énumération devenues obsolètes.
 *
 * <p>Hibernate (ddl-auto=update) génère un CHECK listant les valeurs d'un enum
 * {@code @Enumerated(STRING)} <b>à la création de la table</b>, mais ne le met
 * jamais à jour ensuite. Quand on ajoute une valeur (ex. {@code EMPLOYEE} à
 * {@link com.salahtech.BarberShop_Apis.Enums.SalonRole} et
 * {@link com.salahtech.BarberShop_Apis.Enums.ApplicationUserType}), l'INSERT de
 * la nouvelle valeur échoue (« violates check constraint … »).</p>
 *
 * <p>On supprime donc ces contraintes (la validation reste assurée au niveau
 * applicatif par les enums Java). Idempotent : {@code DROP … IF EXISTS}.</p>
 */
@Slf4j
@Component
@Order(0) // avant les seeders / backfills
@RequiredArgsConstructor
public class EnumConstraintMigration implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        dropCheck("salon_memberships", "salon_memberships_role_check");
        dropCheck("application_users", "application_users_user_type_check");
    }

    private void dropCheck(String table, String constraint) {
        try {
            jdbc.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
            log.info("✅ Contrainte CHECK obsolète supprimée si présente : {}", constraint);
        } catch (Exception e) {
            log.warn("Suppression de la contrainte {} ignorée : {}", constraint, e.getMessage());
        }
    }
}
