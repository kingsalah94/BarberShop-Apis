package com.salahtech.BarberShop_Apis.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RESET de la base (dev) : vide TOUTES les tables puis laisse les initialiseurs
 * recréer un jeu de données propre (admin recréé par SystemAdminInitializer,
 * salon-centric par DemoDataSeeder/DemoOpsSeeder).
 *
 * <p>⚠️ Déclenché uniquement par {@code app.reset-data=true}. À remettre à
 * {@code false} après un démarrage, sinon la base est vidée à chaque lancement.</p>
 *
 * <p>Tourne en TOUT PREMIER (HIGHEST_PRECEDENCE), avant l'admin et les seeders.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class DatabaseResetRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Value("${app.reset-data:false}")
    private boolean resetData;

    @Override
    public void run(String... args) {
        if (!resetData) return;
        // Jamais propager : un échec ici ne doit PAS empêcher le serveur de démarrer.
        try {
            List<String> tables = jdbc.queryForList(
                    "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
            if (tables.isEmpty()) {
                log.warn("♻️  RESET demandé mais aucune table à vider.");
                return;
            }
            String joined = tables.stream().map(t -> "\"" + t + "\"").collect(Collectors.joining(", "));
            // CASCADE gère les clés étrangères ; RESTART IDENTITY remet les séquences à zéro.
            jdbc.execute("TRUNCATE TABLE " + joined + " RESTART IDENTITY CASCADE");
            log.warn("♻️  RESET: {} tables vidées. L'admin et les données de démo vont être recréés. "
                    + "⚠️ Repasse app.reset-data=false pour ne pas revider au prochain démarrage.", tables.size());
        } catch (Exception e) {
            log.error("♻️  RESET échoué (démarrage poursuivi quand même) : {}", e.getMessage(), e);
        }
    }
}
