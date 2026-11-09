package com.salahtech.BarberShop_Apis.migration;

import com.salahtech.BarberShop_Apis.Enums.SalonRole;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.models.SalonMembership;
import com.salahtech.BarberShop_Apis.repositories.RoleRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonMembershipRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Crée le membership OWNER manquant pour chaque salon existant (migration douce
 * vers le modèle de comptes par salon). Idempotent.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class SalonMembershipBackfill implements CommandLineRunner {

    private final SalonRepository salonRepository;
    private final SalonMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        // Rôles globaux pour les sous-comptes.
        ensureRole("MANAGER", "Manager du salon");
        ensureRole("RECEPTIONIST", "Réceptionniste du salon");
        // Rôle Spring du prestataire connecté (espace pro) — accès endpoints staff.
        ensureRole("BARBER", "Barbier / prestataire");

        int created = 0;
        for (Salon salon : salonRepository.findAll()) {
            if (salon.getOwner() == null) continue;
            Long ownerId = salon.getOwner().getId();
            if (!membershipRepository.existsByUser_IdAndSalon_Id(ownerId, salon.getId())) {
                membershipRepository.save(SalonMembership.builder()
                        .user(salon.getOwner())
                        .salon(salon)
                        .role(SalonRole.OWNER)
                        .build());
                created++;
            }
        }
        if (created > 0) log.info("Backfill memberships : {} membership(s) OWNER créé(s)", created);
    }

    private void ensureRole(String name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role r = new Role();
            r.setName(name);
            r.setDescription(description);
            roleRepository.save(r);
            log.info("Rôle global créé : {}", name);
        }
    }
}
