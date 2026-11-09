package com.salahtech.BarberShop_Apis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.repositories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.repositories.RoleRepository;

import jakarta.servlet.http.Cookie;

/**
 * Sécurité de bout en bout (vraie base, vraie chaîne Spring Security) :
 * session web par cookie HttpOnly, isolation entre salons, création de salon.
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
class SecurityAccessIT {

    private static final String PWD = "Motdepasse#2026";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ApplicationUserRepository users;
    @Autowired RoleRepository roles;

    // ---------- outils ----------

    private String newEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@it.test";
    }

    /** Inscription publique (CLIENT) puis connexion mobile (jetons dans le corps). */
    private String registerAndLogin(String email) throws Exception {
        mvc.perform(post("/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PWD,
                                "firstName", "Test", "lastName", "IT", "phone", "+2279" + (1000000 + (int) (Math.random() * 8999999)), "userType", "CLIENT"))))
                .andExpect(status().isCreated());
        return login(email).get("accessToken").asText();
    }

    private JsonNode login(String email) throws Exception {
        MvcResult r = mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PWD))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    /** Change le type de compte et ajoute un rôle Spring (les droits sont relus en base à chaque requête). */
    private void promote(String email, ApplicationUserType type, String roleName) {
        ApplicationUser u = users.findByEmail(email).orElseThrow();
        u.setUserType(type);
        Role role = roles.findByName(roleName).orElseThrow();
        Set<Role> rs = new HashSet<>(u.getRoles());
        rs.add(role);
        u.setRoles(rs);
        users.save(u);
    }

    private long createSalon(String token, String name) throws Exception {
        MvcResult r = mvc.perform(post("/v1/salons").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name, "address", "Niamey"))))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- session web : cookie HttpOnly ----------

    @Test
    void web_refreshTokenEnCookieHttpOnly_jamaisDansLeJson() throws Exception {
        String email = newEmail("web");
        registerAndLogin(email);

        MvcResult r = mvc.perform(post("/v1/auth/login").header("X-Auth-Mode", "cookie")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PWD))))
                .andExpect(status().isOk()).andReturn();

        JsonNode body = json.readTree(r.getResponse().getContentAsString());
        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("refreshToken") == null || body.get("refreshToken").isNull()).isTrue();

        String setCookie = r.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("bb_refresh=").contains("HttpOnly").contains("SameSite=Strict")
                .contains("Path=/api/v1/auth");

        // Rafraîchissement par le seul cookie
        Cookie cookie = r.getResponse().getCookie("bb_refresh");
        MvcResult refreshed = mvc.perform(post("/v1/auth/refresh").header("X-Auth-Mode", "cookie").cookie(cookie))
                .andExpect(status().isOk()).andReturn();
        Cookie rotated = refreshed.getResponse().getCookie("bb_refresh");
        assertThat(rotated).isNotNull();

        // Déconnexion : cookie effacé, le jeton ne sert plus
        MvcResult out = mvc.perform(post("/v1/auth/logout").cookie(rotated))
                .andExpect(status().isNoContent()).andReturn();
        assertThat(out.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
        int after = mvc.perform(post("/v1/auth/refresh").cookie(rotated)).andReturn().getResponse().getStatus();
        assertThat(after).isNotEqualTo(200);
    }

    @Test
    void mobile_sansEnTete_jetonsDansLeCorpsCommeAvant() throws Exception {
        String email = newEmail("mobile");
        registerAndLogin(email);

        JsonNode body = login(email);
        assertThat(body.get("refreshToken").asText()).isNotBlank();
    }

    @Test
    void refreshSansJeton_refuse() throws Exception {
        mvc.perform(post("/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    // ---------- isolation entre salons ----------

    @Test
    void unProprietaireNeVoitPasLeSalonDUnAutre() throws Exception {
        String a = newEmail("ownerA"), b = newEmail("ownerB");
        String tokA = registerAndLogin(a), tokB = registerAndLogin(b);
        promote(a, ApplicationUserType.SALON_OWNER, "SALON_OWNER");
        promote(b, ApplicationUserType.SALON_OWNER, "SALON_OWNER");
        long salonA = createSalon(tokA, "Salon A");
        long salonB = createSalon(tokB, "Salon B");

        mvc.perform(get("/v1/salons/" + salonA + "/bookings").header(HttpHeaders.AUTHORIZATION, bearer(tokA)))
                .andExpect(status().isOk());
        mvc.perform(get("/v1/salons/" + salonB + "/bookings").header(HttpHeaders.AUTHORIZATION, bearer(tokA)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/salons/" + salonB + "/clients").header(HttpHeaders.AUTHORIZATION, bearer(tokA)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/queue/salon/" + salonB).header(HttpHeaders.AUTHORIZATION, bearer(tokA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unClientNAccedeNiALAdministrationNiAuxDonneesDUnSalon() throws Exception {
        String owner = newEmail("owner"), client = newEmail("client");
        String tokOwner = registerAndLogin(owner), tokClient = registerAndLogin(client);
        promote(owner, ApplicationUserType.SALON_OWNER, "SALON_OWNER");
        long salon = createSalon(tokOwner, "Salon C");

        mvc.perform(get("/v1/roles").header(HttpHeaders.AUTHORIZATION, bearer(tokClient)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/salons/" + salon + "/clients").header(HttpHeaders.AUTHORIZATION, bearer(tokClient)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/dashboard/stats").header(HttpHeaders.AUTHORIZATION, bearer(tokClient)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/v1/salons").header(HttpHeaders.AUTHORIZATION, bearer(tokClient))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Pirate\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- création de salon (option A) ----------

    @Test
    void barbierIndependant_creeSonSalon_etDevientProprietaire() throws Exception {
        String email = newEmail("barber");
        String tok = registerAndLogin(email);
        promote(email, ApplicationUserType.BARBER, "BARBER");

        long salon = createSalon(tok, "Mon salon");

        assertThat(users.findByEmail(email).orElseThrow().getUserType()).isEqualTo(ApplicationUserType.SALON_OWNER);
        mvc.perform(get("/v1/salons/" + salon + "/bookings").header(HttpHeaders.AUTHORIZATION, bearer(tok)))
                .andExpect(status().isOk());
        // Il est réservable dans son propre salon
        MvcResult staff = mvc.perform(get("/v1/catalog/salons/" + salon + "/staff")).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(staff.getResponse().getContentAsString()).size()).isEqualTo(1);
    }

    @Test
    void prestataireDUnSalon_nePeutPasCreerDeSalon() throws Exception {
        String email = newEmail("employee");
        String tok = registerAndLogin(email);
        promote(email, ApplicationUserType.EMPLOYEE, "BARBER");

        mvc.perform(post("/v1/salons").header(HttpHeaders.AUTHORIZATION, bearer(tok))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Concurrent\"}"))
                .andExpect(status().isForbidden());
    }
}
