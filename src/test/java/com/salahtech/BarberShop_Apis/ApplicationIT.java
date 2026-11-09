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
 * DB_USERNAME / DB_PASSWORD / JWT_SECRET_BASE64 (ou secrets.properties en local).</p>
 */
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "app.demo-data.enabled=false",
        "app.admin.password="
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
