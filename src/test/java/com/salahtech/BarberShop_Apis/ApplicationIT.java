package com.salahtech.BarberShop_Apis;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Démarrage complet de l'application sur une vraie base PostgreSQL.
 *
 * <p>Sur une base vide (CI), Flyway applique V1 puis Hibernate valide le schéma :
 * ce test échoue si une entité diverge des migrations. Nécessite DB_URL /
 * DB_USERNAME / DB_PASSWORD / JWT_SECRET_BASE64 (ou le fichier .env en local).</p>
 */
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "app.demo-data.enabled=false",
        "app.admin.password=",
        // Jamais d'e-mail réel depuis les tests : SMTP local inexistant (échec immédiat)
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=1",
        "spring.mail.properties.mail.smtp.connectiontimeout=500",
        "spring.mail.properties.mail.smtp.timeout=500"
})
@AutoConfigureMockMvc
class ApplicationIT {

    @Autowired MockMvc mvc;

    @Test
    void sante_ok() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }

    @Test
    void administrationDesRoles_interditeAuxAnonymes() throws Exception {
        mvc.perform(get("/v1/roles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/permissions")).andExpect(status().isUnauthorized());
    }

    @Test
    void fileDAttente_interditeAuxAnonymes() throws Exception {
        mvc.perform(get("/v1/queue/salon/1")).andExpect(status().isUnauthorized());
    }
}
