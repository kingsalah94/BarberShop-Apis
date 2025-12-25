package com.salahtech.BarberShop_Apis.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.RoleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Initialise automatiquement un compte administrateur système au démarrage
 * si aucun utilisateur n'existe dans la base de données.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class SystemAdminInitializer implements CommandLineRunner {

    // ===== DÉPENDANCES =====
    private final ApplicationUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ===== CONFIGURATION =====
    @Value("${app.admin.email:admin@barbershop.local}")
    private String adminEmail;

    @Value("${app.admin.password:Root@Admin@2025}")
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
            displaySystemStats();
            return;
        }

        long userCount = userRepository.count();

        if (userCount > 0) {
            log.info("✔️  {} utilisateur(s) déjà existant(s)", userCount);
            checkSystemHealth();
            displaySystemStats();
        } else {
            log.warn("⚠️  Aucun utilisateur trouvé – création de l'administrateur système");
            initializeRoles();
            createSystemAdmin();

            if (createTestUsers) {
                createDefaultTestUsers();
            }
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ Initialisation du système terminée avec succès");
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

        log.info("✅ Rôles système initialisés");
    }

    private void createRoleIfNotExists(String roleName, String description) {
        if (!roleRepository.existsByName(roleName)) {
            Role role = new Role();
            role.setName(roleName);
            role.setDescription(description);
            roleRepository.save(role);
            log.debug("   ✔️  Rôle créé: {}", roleName);
        }
    }

    // ===== CRÉATION ADMIN =====
    @Transactional
    private void createSystemAdmin() {
        try {
            log.info("👤 Création de l'administrateur système...");

            // Créer l'utilisateur admin
            ApplicationUser admin = new ApplicationUser();
            admin.setFirstName(adminFirstName);
            admin.setLastName(adminLastName);
            admin.setEmail(adminEmail);
            admin.setPhone(adminPhone);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setVerificationToken(UUID.randomUUID().toString());
            admin.setUserType(ApplicationUserType.ADMIN);

            // Assigner le rôle ADMIN
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Rôle ADMIN non trouvé"));
            admin.setRoles(Set.of(adminRole));

            // Activer le compte
            admin.setIsVerified(true);
            admin.setIsEnabled(true);
            admin.setAccountNonLocked(true);
            admin.setCreatedAt(LocalDateTime.now());

            // Sauvegarder
            userRepository.save(admin);

            log.info("✅ Administrateur système créé avec succès");
            log.info("   📧 Email: {}", adminEmail);
            log.info("   👤 Nom: {} {}", adminFirstName, adminLastName);
            log.info("   📞 Téléphone: {}", adminPhone);
            log.warn("   🔐 Mot de passe: {} - ⚠️  CHANGEZ-LE IMMÉDIATEMENT!", adminPassword);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.warn("🚨 IMPORTANT: Changez le mot de passe admin dès la première connexion!");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'administrateur système", e);
            throw new RuntimeException("Échec de l'initialisation: " + e.getMessage(), e);
        }
    }

    // ===== UTILISATEURS DE TEST =====
    @Transactional
    public void createDefaultTestUsers() {
        log.info("🧪 Création d'utilisateurs de test...");

        try {
            if (userRepository.existsByEmail("client@test.local")) {
                log.info("ℹ️  Les utilisateurs de test existent déjà");
                return;
            }

            createTestUser("Test", "Client", "client@test.local", "+123456789",
                    "Client@123", ApplicationUserType.CLIENT, "ROLE_CLIENT");
            createTestUser("Test", "Barber", "barber@test.local", "+987654321",
                    "Barber@123", ApplicationUserType.BARBER, "ROLE_BARBER");
            createTestUser("Test", "Owner", "owner@test.local", "+555555555",
                    "Owner@123", ApplicationUserType.SALON_OWNER, "ROLE_SALON_OWNER");

            log.info("✅ Utilisateurs de test créés avec succès");
            log.info("   📧 Client: client@test.local / Client@123");
            log.info("   📧 Barber: barber@test.local / Barber@123");
            log.info("   📧 Owner: owner@test.local / Owner@123");

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création des utilisateurs de test", e);
        }
    }

    @Transactional
    private void createTestUser(String firstName, String lastName, String email,
            String phone, String password, ApplicationUserType userType,
            String roleName) {
        try {
            if (userRepository.existsByEmail(email)) {
                log.debug("⏭️  L'utilisateur {} existe déjà", email);
                return;
            }

            ApplicationUser user = new ApplicationUser();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(password));
            user.setUserType(userType);
            user.setVerificationToken(UUID.randomUUID().toString());

            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Rôle " + roleName + " non trouvé"));
            user.setRoles(Set.of(role));

            user.setIsVerified(true);
            user.setIsEnabled(true);
            user.setAccountNonLocked(true);
            user.setCreatedAt(LocalDateTime.now());

            userRepository.save(user);
            log.debug("✔️  Utilisateur de test créé: {} ({})", email, roleName);

        } catch (Exception e) {
            log.error("❌ Erreur création utilisateur {}: {}", email, e.getMessage());
        }
    }

    // ===== VÉRIFICATIONS SYSTÈME =====
    private void checkSystemHealth() {
        log.info("🔍 Vérification de la santé du système...");
        checkConfiguredAdmin();
        checkAdminCount();
        checkLockedAccounts();
        checkUnverifiedEmails();
    }

    private void checkConfiguredAdmin() {
        Optional<ApplicationUser> admin = userRepository.findByEmail(adminEmail);

        if (admin.isPresent()) {
            ApplicationUser user = admin.get();
            log.info("✔️  Compte admin configuré trouvé: {}", adminEmail);

            boolean hasAdminRole = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));

            if (!hasAdminRole) {
                log.warn("⚠️  L'utilisateur {} n'a pas le rôle ADMIN!", adminEmail);
            }

            if (!user.getIsEnabled()) {
                log.error("🚨 L'administrateur {} est désactivé!", adminEmail);
            }

            if (!user.isAccountNonLocked()) {
                log.error("🚨 L'administrateur {} est verrouillé!", adminEmail);
            }
        } else {
            log.warn("⚠️  Aucun compte admin avec l'email configuré: {}", adminEmail);
        }
    }

    private void checkAdminCount() {
        long adminCount = userRepository.countByRolesContaining("ROLE_ADMIN");

        if (adminCount == 0) {
            log.error("🚨 ATTENTION: Aucun administrateur dans le système!");
        } else if (adminCount == 1) {
            log.warn("⚠️  Un seul administrateur dans le système");
            log.info("💡 Conseil: Créez au moins un compte admin de secours");
        } else {
            log.info("✔️  {} administrateur(s) actif(s)", adminCount);
        }
    }

    private void checkLockedAccounts() {
        long lockedCount = userRepository.countByAccountNonLockedFalse();
        if (lockedCount > 0) {
            log.warn("⚠️  {} compte(s) verrouillé(s)", lockedCount);
        }
    }

    private void checkUnverifiedEmails() {
        long unverifiedCount = userRepository.countByIsVerifiedFalse();
        if (unverifiedCount > 0) {
            log.info("ℹ️  {} utilisateur(s) avec email non vérifié", unverifiedCount);
        }
    }

    // ===== STATISTIQUES =====
    private void displaySystemStats() {
        log.info("📊 Statistiques du système:");

        long totalUsers = userRepository.count();
        long adminCount = userRepository.countByRolesContaining("ROLE_ADMIN");
        long barberCount = userRepository.countByRolesContaining("ROLE_BARBER");
        long clientCount = userRepository.countByRolesContaining("ROLE_CLIENT");
        long ownerCount = userRepository.countByRolesContaining("ROLE_SALON_OWNER");
        long enabledCount = userRepository.countByIsEnabledTrue();
        long disabledCount = userRepository.countByIsEnabledFalse();

        log.info("   👥 Total: {}", totalUsers);
        log.info("   👔 Admins: {}", adminCount);
        log.info("   ✂️  Barbers: {}", barberCount);
        log.info("   👤 Clients: {}", clientCount);
        log.info("   🏢 Owners: {}", ownerCount);
        log.info("   ✅ Actifs: {}", enabledCount);
        log.info("   ❌ Désactivés: {}", disabledCount);
    }

    // ===== MÉTHODES UTILITAIRES =====
    @Transactional
    public void resetAdminPassword(String newPassword) {
        log.info("🔐 Réinitialisation du mot de passe admin...");

        ApplicationUser admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setUpdatedAt(LocalDateTime.now());
        userRepository.save(admin);

        log.info("✅ Mot de passe admin réinitialisé");
    }

    @Transactional
    public void toggleUserStatus(String email, boolean enabled) {
        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setIsEnabled(enabled);
        userRepository.save(user);

        log.info("{} Utilisateur {} {}",
                enabled ? "✅" : "❌", email, enabled ? "activé" : "désactivé");
    }

    public boolean needsInitialization() {
        return userRepository.count() == 0;
    }

    public long getAdminCount() {
        return userRepository.countByRolesContaining("ROLE_ADMIN");
    }

    public boolean adminExists(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getRoles().stream()
                        .anyMatch(role -> "ROLE_ADMIN".equals(role.getName())))
                .orElse(false);
    }
}