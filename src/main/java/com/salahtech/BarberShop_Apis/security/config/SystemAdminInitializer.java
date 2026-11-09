package com.salahtech.BarberShop_Apis.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.repositories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.repositories.BarberRepository;
import com.salahtech.BarberShop_Apis.repositories.ClientRepository;
import com.salahtech.BarberShop_Apis.repositories.RoleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Initialise automatiquement les comptes système et de test au démarrage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class SystemAdminInitializer implements CommandLineRunner {

    // ===== DÉPENDANCES =====
    private final ApplicationUserRepository userRepository;
    private final RoleRepository roleRepository;
    // ✅ AJOUTS : Repositories pour créer les profils métiers
    private final ClientRepository clientRepository;
    private final BarberRepository barberRepository;

    private final PasswordEncoder passwordEncoder;

    // ===== CONFIGURATION =====
    @Value("${app.admin.email:admin@barbershop.local}")
    private String adminEmail;

    @Value("${app.admin.password:}") // aucun mot de passe par défaut : à fournir via APP_ADMIN_PASSWORD
    private String adminPassword;

    @Value("${app.admin.firstName:System}")
    private String adminFirstName;

    @Value("${app.admin.lastName:Administrator}")
    private String adminLastName;

    @Value("${app.admin.phone:+000000000000}")
    private String adminPhone;

    @Value("${app.admin.auto-create:true}")
    private boolean autoCreateAdmin;

    @Value("${app.test-users.enabled:false}")
    private boolean createTestUsers;

    // ===== INITIALISATION PRINCIPALE =====
    @Override
    @Transactional
    public void run(String... args) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 Démarrage de l'initialisation du système BarberShop");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (!autoCreateAdmin) {
            log.info("ℹ️  Auto-création d'admin désactivée");
            return;
        }

        long userCount = userRepository.count();

        if (userCount > 0) {
            log.info("✔️  {} utilisateur(s) déjà existant(s)", userCount);
            // On ne fait rien si la base n'est pas vide pour éviter les doublons
        } else {
            log.warn("⚠️  Aucun utilisateur trouvé – création de l'administrateur système");
            initializeRoles();

            // 1. Création de l'Admin (SANS profil Client)
            createSystemAdmin();

            // 2. Création des users de test (AVEC profils Client/Barber)
            if (createTestUsers) {
                createDefaultTestUsers();
            }
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ Initialisation terminée");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ===== INITIALISATION DES RÔLES =====
    @Transactional
    private void initializeRoles() {
        log.info("🔐 Initialisation des rôles système...");
        createRoleIfNotExists("ADMIN", "Administrator with full system access");
        createRoleIfNotExists("SALON_OWNER", "Salon owner with management privileges");
        createRoleIfNotExists("BARBER", "Barber with service privileges");
        createRoleIfNotExists("CLIENT", "Client with booking privileges");
    }

    private void createRoleIfNotExists(String roleName, String description) {
        if (!roleRepository.existsByName(roleName)) {
            Role role = new Role();
            role.setName(roleName);
            role.setDescription(description);
            roleRepository.save(role);
        }
    }

    // ===== CRÉATION ADMIN (PURE) =====
    @Transactional
    private void createSystemAdmin() {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.error("❌ app.admin.password (APP_ADMIN_PASSWORD) non défini : admin système NON créé.");
            return;
        }
        try {
            ApplicationUser admin = new ApplicationUser();
            admin.setFirstName(adminFirstName);
            admin.setLastName(adminLastName);
            admin.setEmail(adminEmail);
            admin.setPhone(adminPhone);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setMustChangePassword(true);
            admin.setVerificationToken(UUID.randomUUID().toString());
            admin.setUserType(ApplicationUserType.ADMIN);

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Rôle ADMIN non trouvé"));
            admin.setRoles(Set.of(adminRole));

            admin.setIsVerified(true);
            admin.setIsEnabled(true);
            admin.setAccountNonLocked(true);
            admin.setCreatedAt(LocalDateTime.now());

            userRepository.save(admin);
            // ⚠️ NOTE : On ne crée PAS de "new Client(admin)" ici. L'admin n'est pas un
            // client.

            log.info("✅ Administrateur système créé: {}", adminEmail);

        } catch (Exception e) {
            log.error("❌ Erreur création admin", e);
        }
    }

    // ===== UTILISATEURS DE TEST (COMPLETS) =====
    @Transactional
    public void createDefaultTestUsers() {
        log.info("🧪 Création d'utilisateurs de test...");

        // Client Test
        createTestUser("Test", "Client", "client@test.local", "+123456789",
                "Client@123", ApplicationUserType.CLIENT, "ROLE_CLIENT");

        // Barbier Test
        createTestUser("Test", "Barber", "barber@test.local", "+987654321",
                "Barber@123", ApplicationUserType.BARBER, "ROLE_BARBER");

        // Owner Test
        createTestUser("Test", "Owner", "owner@test.local", "+555555555",
                "Owner@123", ApplicationUserType.SALON_OWNER, "ROLE_SALON_OWNER");
    }

    @Transactional
    private void createTestUser(String firstName, String lastName, String email,
            String phone, String password, ApplicationUserType userType,
            String roleName) {
        try {
            if (userRepository.existsByEmail(email))
                return;

            // 1. Création du User (Compte de connexion)
            ApplicationUser user = new ApplicationUser();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(password));
            user.setMustChangePassword(false); // Pas besoin de changer pour les tests
            user.setUserType(userType);
            user.setVerificationToken(UUID.randomUUID().toString());

            Role role = roleRepository.findByName(roleName.replace("ROLE_", "")) // "CLIENT" au lieu de "ROLE_CLIENT" si
                                                                                 // stocké sans prefixe
                    .orElseThrow(() -> new RuntimeException("Rôle " + roleName + " non trouvé"));
            user.setRoles(Set.of(role));

            user.setIsVerified(true);
            user.setIsEnabled(true);
            user.setAccountNonLocked(true);
            user.setCreatedAt(LocalDateTime.now());

            ApplicationUser savedUser = userRepository.save(user);

            // 2. ✅ CRÉATION DU PROFIL MÉTIER ASSOCIÉ
            if (userType == ApplicationUserType.CLIENT) {
                Client client = new Client();
                client.setUser(savedUser);
                client.setPhone(phone);
                client.setFirstName(firstName); // Si ces champs existent dans Client
                client.setLastName(lastName);
                clientRepository.save(client);
                log.info("   👤 Profil Client créé pour {}", email);
            } else if (userType == ApplicationUserType.BARBER) {
                Barber barber = new Barber();
                barber.setUser(savedUser);
                barber.setName(firstName + " " + lastName); // Champ name du barbier
                barber.setPhoneNumber(phone);
                barber.setAvailable(true);
                barberRepository.save(barber);
                log.info("   ✂️ Profil Barbier créé pour {}", email);
            }

            log.debug("✔️ Utilisateur complet créé: {}", email);

        } catch (Exception e) {
            log.error("❌ Erreur création utilisateur {}: {}", email, e.getMessage());
        }
    }
}