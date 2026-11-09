package com.salahtech.BarberShop_Apis.migration;

import com.salahtech.BarberShop_Apis.Dtos.CreateEmployeeRequest;
import com.salahtech.BarberShop_Apis.Dtos.EmployeeDto;
import com.salahtech.BarberShop_Apis.Dtos.QueueEntryDto;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.EmployeeRole;
import com.salahtech.BarberShop_Apis.Enums.SalonRole;
import com.salahtech.BarberShop_Apis.Enums.StationStatus;
import com.salahtech.BarberShop_Apis.Services.Interfaces.EmployeeService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.QueueService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.WorkstationService;
import com.salahtech.BarberShop_Apis.models.*;
import com.salahtech.BarberShop_Apis.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Données OPÉRATIONNELLES de démo pour le salon owner@demo : prestataires (avec
 * accès) attribués aux postes, RDV du jour (réalisés / en cours / à venir / à
 * valider) et file d'attente. Sans ça, les écrans staff sont vides (le seeder de
 * base ne crée ni employés, ni réservations, ni file).
 *
 * <p>Idempotent : ne fait rien si le salon a déjà des réservations aujourd'hui.</p>
 */
@Slf4j
@Component
@Order(10) // après DemoDataSeeder (2) + EnumConstraintMigration (0)
@RequiredArgsConstructor
public class DemoOpsSeeder implements CommandLineRunner {

    private final ApplicationUserRepository userRepository;
    private final SalonRepository salonRepository;
    private final ClientRepository clientRepository;
    private final WorkstationRepository workstationRepository;
    private final SalonOfferingRepository offeringRepository;
    private final SalonBookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final SalonMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeService employeeService;
    private final WorkstationService workstationService;
    private final QueueService queueService;

    @org.springframework.beans.factory.annotation.Value("${app.demo-data.enabled:false}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) return;
        try {
            ApplicationUser owner = userRepository.findByEmail("owner@demo.barbershop").orElse(null);
            if (owner == null) return;
            Salon salon = salonRepository.findByOwner_Id(owner.getId()).stream().findFirst().orElse(null);
            if (salon == null) return;
            final Long sid = salon.getId();

            // Sous-comptes de gestion garantis À CHAQUE démarrage (indépendant du marqueur
            // du seeder de base) : sinon manager/réception peuvent ne jamais être créés.
            ensureSubAccount("manager@demo.barbershop", "Mehdi", "Manager", "Manager@123",
                    ApplicationUserType.MANAGER, "MANAGER", salon, SalonRole.MANAGER);
            ensureSubAccount("receptionist@demo.barbershop", "Rita", "Réception", "Reception@123",
                    ApplicationUserType.RECEPTIONIST, "RECEPTIONIST", salon, SalonRole.RECEPTIONIST);

            // Postes (créés par le seeder de base ; sécurité : on en crée si absents) — TOUJOURS.
            List<Workstation> postes = workstationRepository.findBySalon_IdOrderByDisplayOrderAscIdAsc(sid);
            if (postes.isEmpty()) {
                int order = 0;
                for (String name : List.of("Fauteuil 1", "Fauteuil 2", "Bac à shampoing")) {
                    workstationRepository.save(Workstation.builder()
                            .name(name).salon(salon).status(StationStatus.FREE)
                            .displayOrder(order++).isActive(true).build());
                }
                postes = workstationRepository.findBySalon_IdOrderByDisplayOrderAscIdAsc(sid);
            }

            // Prestataires (BARBIERS) avec accès + attribution aux postes — TOUJOURS (idempotent),
            // AVANT le gate des RDV : les barbiers doivent exister même si des RDV existent déjà.
            String[][] presta = {
                    {"Karim", "Benali", "presta.karim@demo.barbershop"},
                    {"Tom", "Lefèvre", "presta.tom@demo.barbershop"},
                    {"Yacine", "Touré", "presta.yacine@demo.barbershop"},
            };
            List<Employee> employees = new ArrayList<>();
            for (String[] p : presta) {
                Employee e = ensurePresta(sid, p[0], p[1], p[2]);
                if (e != null) employees.add(e);
            }
            for (int i = 0; i < employees.size() && i < postes.size(); i++) {
                try {
                    workstationService.assignEmployee(postes.get(i).getId(), employees.get(i).getId());
                } catch (Exception ex) {
                    log.warn("Attribution poste {} → presta {} ignorée : {}", postes.get(i).getId(), employees.get(i).getId(), ex.getMessage());
                }
            }

            // RDV + file : seulement si aucun RDV aujourd'hui (idempotence des données du jour).
            LocalDate today = LocalDate.now();
            if (!bookingRepository.findBySalon_IdAndStartAtBetweenOrderByStartAtAsc(
                    sid, today.atStartOfDay(), today.plusDays(1).atStartOfDay()).isEmpty()) {
                log.info("✅ Prestataires & postes assurés ; RDV du jour déjà présents — pas de reseed des RDV.");
                return;
            }

            // Offres + client de démo (porteurs des RDV).
            List<SalonOffering> offers = offeringRepository.findBySalon_IdAndActiveTrueOrderByNameAsc(sid);
            if (offers.isEmpty()) return;
            ApplicationUser clientUser = userRepository.findByEmail("client@demo.barbershop").orElse(null);
            Client client = clientUser == null ? null : clientRepository.findByUser_Id(clientUser.getId()).orElse(null);
            if (client == null) return;

            final Employee e0 = employees.isEmpty() ? null : employees.get(0);
            final Employee e1 = employees.size() > 1 ? employees.get(1) : e0;
            final Employee e2 = employees.size() > 2 ? employees.get(2) : e1;
            final Workstation w0 = postes.get(0);
            final Workstation w1 = postes.size() > 1 ? postes.get(1) : w0;

            SalonOffering o0 = offers.get(0);
            SalonOffering o1 = offers.size() > 1 ? offers.get(1) : o0;
            SalonOffering o2 = offers.size() > 2 ? offers.get(2) : o0;
            LocalDateTime now = LocalDateTime.now();

            // 2) RDV réalisés ce matin.
            save(salon, o0, client, today.atTime(LocalTime.of(9, 0)), BookingStatus.COMPLETED, e0, w0);
            save(salon, o1, client, today.atTime(LocalTime.of(10, 30)), BookingStatus.COMPLETED, e1, w1);

            // 3) RDV en cours (sera mis en service dans la file).
            SalonBooking inProgress = save(salon, o1, client, now.minusMinutes(10), BookingStatus.CONFIRMED, e0, w0);

            // 4) RDV à venir confirmé (ira en file d'attente).
            SalonBooking upcoming = save(salon, o0, client, now.plusMinutes(40), BookingStatus.CONFIRMED, e1, w1);

            // 5) Demandes à valider (PENDING).
            save(salon, o2, client, now.plusMinutes(90), BookingStatus.PENDING, null, null);
            save(salon, o0, client, now.plusMinutes(150), BookingStatus.PENDING, null, null);

            // 6) File d'attente : 1 en service (IN_CHAIR) + 1 en attente (WAITING).
            try {
                QueueEntryDto entry = queueService.enqueueFromSalonBooking(inProgress.getId());
                queueService.callSpecific(entry.getId(), w0.getId());
                queueService.startService(entry.getId());
            } catch (Exception ex) {
                log.warn("File (en service) démo ignorée : {}", ex.getMessage());
            }
            try {
                queueService.enqueueFromSalonBooking(upcoming.getId());
            } catch (Exception ex) {
                log.warn("File (en attente) démo ignorée : {}", ex.getMessage());
            }

            log.info("✅ Données opérationnelles démo créées pour salon {} ({} prestataires, RDV du jour + file).", sid, employees.size());
        } catch (Exception e) {
            log.error("❌ Échec DemoOpsSeeder : {}", e.getMessage(), e);
        }
    }

    /** Garantit un sous-compte de gestion (manager/réception) opérationnel + son membership. */
    private void ensureSubAccount(String email, String first, String last, String password,
            ApplicationUserType type, String roleName, Salon salon, SalonRole salonRole) {
        ApplicationUser u = userRepository.findByEmail(email).orElse(null);
        if (u == null) {
            u = new ApplicationUser();
            u.setFirstName(first);
            u.setLastName(last);
            u.setEmail(email);
            u.setVerificationToken(UUID.randomUUID().toString());
            u.setCreatedAt(LocalDateTime.now());
        }
        // (Ré)assure des identifiants de démo fonctionnels (compte démo).
        u.setPassword(passwordEncoder.encode(password));
        u.setMustChangePassword(false);
        u.setUserType(type);
        Role role = roleRepository.findByName(roleName).orElse(null);
        if (role != null) u.setRoles(Set.of(role));
        u.setIsVerified(true);
        u.setIsEnabled(true);
        u.setAccountNonLocked(true);
        ApplicationUser saved = userRepository.save(u);
        if (!membershipRepository.existsByUser_IdAndSalon_Id(saved.getId(), salon.getId())) {
            membershipRepository.save(SalonMembership.builder().user(saved).salon(salon).role(salonRole).build());
        }
        log.info("   ✅ Sous-compte démo assuré : {}", email);
    }

    /** Crée un prestataire avec accès (ou récupère l'existant). */
    private Employee ensurePresta(Long salonId, String first, String last, String email) {
        ApplicationUser existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            return employeeRepository.findByUser_Id(existing.getId()).orElse(null);
        }
        try {
            CreateEmployeeRequest req = new CreateEmployeeRequest();
            req.setFirstName(first);
            req.setLastName(last);
            req.setEmail(email);
            req.setRole(EmployeeRole.BARBER);
            req.setSalonIds(Set.of(salonId));
            req.setPrimarySalonId(salonId);
            req.setCreateLogin(true);
            req.setLoginPassword("Presta@123");
            EmployeeDto dto = employeeService.create(req);
            log.info("   ✂️  Prestataire démo : {}", email);
            return employeeRepository.findById(dto.getId()).orElse(null);
        } catch (Exception ex) {
            log.warn("Création prestataire {} ignorée : {}", email, ex.getMessage());
            return null;
        }
    }

    private SalonBooking save(Salon salon, SalonOffering offering, Client client, LocalDateTime start,
                              BookingStatus status, Employee employee, Workstation ws) {
        int dur = offering.getDurationMinutes() != null ? offering.getDurationMinutes() : 30;
        return bookingRepository.save(SalonBooking.builder()
                .salon(salon)
                .offering(offering)
                .client(client)
                .startAt(start)
                .endAt(start.plusMinutes(dur))
                .status(status)
                .totalPrice(offering.getPrice())
                .assignedEmployee(employee)
                .assignedWorkstation(ws)
                .build());
    }
}
