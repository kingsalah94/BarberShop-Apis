package com.salahtech.BarberShop_Apis.migration;

import org.springframework.context.annotation.Profile;

import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.SalonRole;
import com.salahtech.BarberShop_Apis.Enums.StationStatus;
import com.salahtech.BarberShop_Apis.models.*;
import com.salahtech.BarberShop_Apis.repositories.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Injecte des données de démonstration (barbiers + prestations + disponibilités)
 * pour pouvoir tester l'app mobile sur des données réelles.
 *
 * Idempotent : ne s'exécute qu'une fois (détecté via l'email du 1er barbier démo).
 * Désactivable via app.demo-data.enabled=false
 */
@Component
@Profile("!prod") // jamais en production, quelle que soit la configuration
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class DemoDataSeeder implements CommandLineRunner {

    private final ApplicationUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BarberRepository barberRepository;
    private final CategoryRepository categoryRepository;
    private final ClientRepository clientRepository;
    private final SalonRepository salonRepository;
    private final SalonHoursRepository salonHoursRepository;
    private final SalonOfferingRepository salonOfferingRepository;
    private final WorkstationRepository workstationRepository;
    private final SalonMembershipRepository salonMembershipRepository;
    private final SalonPostRepository salonPostRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-data.enabled:false}") // démo désactivée par défaut (activer en local uniquement)
    private boolean enabled;

    private static final String MARKER_EMAIL = "marco@demo.barbershop";
    private static final String DEMO_PASSWORD = "Barber@123";

    @Override
    public void run(String... args) {
        if (!enabled) return;

        ensureRole("BARBER", "Barber with service privileges");
        ensureRole("CLIENT", "Client with booking privileges");
        ensureRole("SALON_OWNER", "Salon owner / manager privileges");
        ensureRole("MANAGER", "Salon manager sub-account");
        ensureRole("RECEPTIONIST", "Salon receptionist sub-account");

        // Compte client de démo (indépendant des barbiers) pour tester l'app mobile.
        ensureDemoClient();

        // Idempotence : salon de démo déjà présent ? (basé sur le salon, pas sur les ex-barbiers)
        ApplicationUser existingOwner = userRepository.findByEmail("owner@demo.barbershop").orElse(null);
        if (existingOwner != null && !salonRepository.findByOwner_Id(existingOwner.getId()).isEmpty()) {
            log.info("✔️  Salon de démo déjà présent — seeding ignoré");
            return;
        }
        log.info("🌱 Seeding salon-centric (nouvelle architecture)...");

        final Category category = ensureCategory();

        // Propriétaire de salon (tenant) + salon de démo.
        final Salon salon = createDemoOwnerAndSalon();

        // Horaires + services (images) + postes + sous-comptes (manager/réception) + réalisations.
        seedSalonData(salon, category);

        log.info("✅ Données salon-centric créées (owner + manager + réception + services + postes). "
                + "Prestataires / RDV / file ajoutés par DemoOpsSeeder.");
    }

    // ---------- Helpers ----------

    /** Crée un compte client de démonstration (avec profil Client) si absent. */
    private void ensureDemoClient() {
        final String email = "client@demo.barbershop";
        if (userRepository.existsByEmail(email)) return;
        try {
            ApplicationUser user = new ApplicationUser();
            user.setFirstName("Salah");
            user.setLastName("Demo");
            user.setEmail(email);
            user.setPhone("+33600000000");
            user.setPassword(passwordEncoder.encode("Client@123"));
            user.setMustChangePassword(false);
            user.setUserType(ApplicationUserType.CLIENT);
            user.setVerificationToken(UUID.randomUUID().toString());
            roleRepository.findByName("CLIENT").ifPresent(r -> user.setRoles(Set.of(r)));
            user.setIsVerified(true);
            user.setIsEnabled(true);
            user.setAccountNonLocked(true);
            user.setCreatedAt(LocalDateTime.now());
            ApplicationUser savedUser = userRepository.save(user);

            Client client = new Client();
            client.setUser(savedUser);
            client.setFirstName("Salah");
            client.setLastName("Demo");
            client.setEmail(email);
            client.setPhone("+33600000000");
            clientRepository.save(client);

            log.info("   👤 Compte client de démo créé : {}", email);
        } catch (Exception e) {
            log.error("   ❌ Échec création client démo : {}", e.getMessage(), e);
        }
    }

    /**
     * Crée un compte propriétaire de salon (SALON_OWNER) + son salon de démo.
     * Login : owner@demo.barbershop / Owner@123
     */
    private Salon createDemoOwnerAndSalon() {
        final String email = "owner@demo.barbershop";

        ApplicationUser owner;
        if (userRepository.existsByEmail(email)) {
            owner = userRepository.findByEmail(email).orElse(null);
        } else {
            ApplicationUser u = new ApplicationUser();
            u.setFirstName("Olivier");
            u.setLastName("Gérant");
            u.setEmail(email);
            u.setPhone("+33611223344");
            u.setPassword(passwordEncoder.encode("Owner@123"));
            u.setMustChangePassword(false);
            u.setUserType(ApplicationUserType.SALON_OWNER);
            u.setVerificationToken(UUID.randomUUID().toString());
            roleRepository.findByName("SALON_OWNER").ifPresent(r -> u.setRoles(Set.of(r)));
            u.setIsVerified(true);
            u.setIsEnabled(true);
            u.setAccountNonLocked(true);
            u.setCreatedAt(LocalDateTime.now());
            owner = userRepository.save(u);
            log.info("   🏢 Propriétaire de salon démo créé : {}", email);
        }

        Salon salon = new Salon();
        salon.setName("BarberShop Oberkampf");
        salon.setAddress("8 rue Oberkampf, Paris");
        salon.setPhoneNumber("+33140000000");
        salon.setOwner(owner);
        salon = salonRepository.save(salon);
        log.info("   🏠 Salon démo créé : {} (owner={})", salon.getName(), email);
        return salon;
    }

    /** Données orientées salon : horaires, services (avec images), postes, sous-comptes, réalisations. */
    private void seedSalonData(Salon salon, Category category) {
        try {
            // Idempotence : on ne seed qu'une fois.
            if (!salonOfferingRepository.findBySalon_IdOrderByNameAsc(salon.getId()).isEmpty()) return;

            // 1) Horaires : Lun-Ven 09:00-19:00, Sam 10:00-17:00, Dim fermé.
            for (DayOfWeek d : DayOfWeek.values()) {
                boolean sat = d == DayOfWeek.SATURDAY;
                boolean sun = d == DayOfWeek.SUNDAY;
                salonHoursRepository.save(SalonHours.builder()
                        .salon(salon).dayOfWeek(d).closed(sun)
                        .openAt(sun ? null : LocalTime.of(sat ? 10 : 9, 0))
                        .closeAt(sun ? null : LocalTime.of(sat ? 17 : 19, 0))
                        .build());
            }

            // 2) Services (images Unsplash, prix FCFA).
            List<String[]> svcs = List.of(
                    new String[]{"Coupe homme", "3000", "30", "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=600"},
                    new String[]{"Coupe + barbe", "5000", "50", "https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=600"},
                    new String[]{"Taille de barbe", "2000", "20", "https://images.unsplash.com/photo-1621607512214-68297480165e?w=600"},
                    new String[]{"Rasage traditionnel", "4000", "40", "https://images.unsplash.com/photo-1585747860715-2ba37e788b70?w=600"},
                    new String[]{"Coupe enfant", "2500", "25", "https://images.unsplash.com/photo-1622286342621-4bd786c2447c?w=600"});
            for (String[] s : svcs) {
                salonOfferingRepository.save(SalonOffering.builder()
                        .salon(salon).name(s[0]).description(s[0])
                        .price(new BigDecimal(s[1])).durationMinutes(Integer.parseInt(s[2]))
                        .imageUrl(s[3]).category(category).active(true).build());
            }

            // 3) Postes.
            int order = 0;
            for (String name : List.of("Fauteuil 1", "Fauteuil 2", "Bac à shampoing")) {
                workstationRepository.save(Workstation.builder()
                        .name(name).salon(salon).status(StationStatus.FREE)
                        .displayOrder(order++).isActive(true).build());
            }

            // 4) Sous-comptes (manager + réceptionniste) + memberships.
            ApplicationUser owner = salon.getOwner();
            if (owner != null) ensureMembership(owner, salon, SalonRole.OWNER);
            ApplicationUser manager = createSubAccount("manager@demo.barbershop", "Mehdi", "Manager",
                    "Manager@123", ApplicationUserType.MANAGER, "MANAGER");
            if (manager != null) ensureMembership(manager, salon, SalonRole.MANAGER);
            ApplicationUser recep = createSubAccount("receptionist@demo.barbershop", "Rita", "Réception",
                    "Reception@123", ApplicationUserType.RECEPTIONIST, "RECEPTIONIST");
            if (recep != null) ensureMembership(recep, salon, SalonRole.RECEPTIONIST);

            // 5) Réalisations (posts) avec images.
            List<String[]> posts = List.of(
                    new String[]{"https://images.unsplash.com/photo-1605497788044-5a32c7078486?w=600", "Dégradé net + barbe sculptée"},
                    new String[]{"https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=600", "Coupe classique au rasoir"},
                    new String[]{"https://images.unsplash.com/photo-1622286342621-4bd786c2447c?w=600", "Finition impeccable"});
            for (String[] p : posts) {
                salonPostRepository.save(SalonPost.builder().salon(salon).imageUrl(p[0]).caption(p[1]).build());
            }

            log.info("   🪑 Salon enrichi : horaires + {} services (images) + 3 postes + sous-comptes + {} réalisations",
                    svcs.size(), posts.size());
        } catch (Exception e) {
            log.error("   ❌ Échec seed salon-centric : {}", e.getMessage(), e);
        }
    }

    /** Crée un sous-compte de gestion (manager/réceptionniste), mot de passe direct (démo). */
    private ApplicationUser createSubAccount(String email, String first, String last,
            String password, ApplicationUserType type, String roleName) {
        if (userRepository.existsByEmail(email)) return userRepository.findByEmail(email).orElse(null);
        ApplicationUser u = new ApplicationUser();
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setMustChangePassword(false);
        u.setUserType(type);
        u.setVerificationToken(UUID.randomUUID().toString());
        roleRepository.findByName(roleName).ifPresent(r -> u.setRoles(Set.of(r)));
        u.setIsVerified(true);
        u.setIsEnabled(true);
        u.setAccountNonLocked(true);
        u.setCreatedAt(LocalDateTime.now());
        ApplicationUser saved = userRepository.save(u);
        log.info("   👥 Sous-compte démo : {}", email);
        return saved;
    }

    private void ensureMembership(ApplicationUser user, Salon salon, SalonRole role) {
        if (salonMembershipRepository.existsByUser_IdAndSalon_Id(user.getId(), salon.getId())) return;
        salonMembershipRepository.save(SalonMembership.builder().user(user).salon(salon).role(role).build());
    }

    private void ensureRole(String name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role r = new Role();
            r.setName(name);
            r.setDescription(description);
            roleRepository.save(r);
        }
    }

    private Category ensureCategory() {
        Category c = new Category();
        c.setName("Coiffure & Barbe");
        c.setDescription("Prestations de coiffure et de barbe");
        c.setIcon("cut-outline");
        c.setIsActive(true);
        return categoryRepository.save(c);
    }

    /** Définition d'une prestation à créer. */
    private record ServiceDef(String name, int price, int duration) {}

    private static ServiceDef svc(String name, int price, int durationMinutes) {
        return new ServiceDef(name, price, durationMinutes);
    }

    private void createBarber(String email, String firstName, String lastName, String phone,
            String salonName, String address, String city, int experienceYears,
            String rating, int reviewsCount, String priceFrom, String description,
            String imageUrl, Category category, Salon salon, List<ServiceDef> services) {
        try {
            // 1. Compte utilisateur (connexion barbier)
            ApplicationUser user = new ApplicationUser();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
            user.setMustChangePassword(false);
            user.setUserType(ApplicationUserType.BARBER);
            user.setVerificationToken(UUID.randomUUID().toString());
            user.setProfileImageUrl(imageUrl);
            roleRepository.findByName("BARBER").ifPresent(r -> user.setRoles(Set.of(r)));
            user.setIsVerified(true);
            user.setIsEnabled(true);
            user.setAccountNonLocked(true);
            user.setCreatedAt(LocalDateTime.now());
            ApplicationUser savedUser = userRepository.save(user);

            // 2. Profil barbier
            Barber barber = new Barber();
            barber.setUser(savedUser);
            barber.setName(firstName + " " + lastName);
            barber.setSalonName(salonName);
            barber.setSalon(salon); // rattachement tenant
            barber.setAddress(address);
            barber.setLocation(address + " · " + city);
            barber.setCity(city);
            barber.setPhoneNumber(phone);
            barber.setExperienceYears(experienceYears);
            barber.setRating(new BigDecimal(rating));
            barber.setReviewsCount(reviewsCount);
            barber.setPriceFrom(new BigDecimal(priceFrom));
            barber.setDescription(description);
            barber.setProfileImageUrl(imageUrl);
            barber.setAvailable(true);
            barber.setIsActive(true);
            barber.setIsVerified(true);
            barber.setCreatedAt(LocalDateTime.now());
            Barber savedBarber = barberRepository.save(barber);

            log.info("   ✂️  Barbier démo créé : {}", salonName);
        } catch (Exception e) {
            log.error("   ❌ Échec création barbier démo {} : {}", email, e.getMessage(), e);
        }
    }
}
