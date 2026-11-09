package com.salahtech.BarberShop_Apis.Config;

import com.salahtech.BarberShop_Apis.Enums.SalonRole;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.repositories.SalonMembershipRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Isolation multi-tenant (tenant = Salon).
 *
 * <p>Le scoping est <b>applicatif</b> : les flux publics/clients
 * (catalogue barbiers, recherche, réservation) restent volontairement
 * cross-tenant. Ce garde n'est appelé que sur les endpoints de
 * <b>gestion</b> (owner / barbier / employé).</p>
 *
 * <p>Règles d'accès :</p>
 * <ul>
 *   <li>ADMIN : accès total (super-admin), aucun filtre.</li>
 *   <li>SALON_OWNER : uniquement les salons dont il est propriétaire.</li>
 *   <li>BARBER : uniquement le salon auquel son profil est rattaché.</li>
 *   <li>autres : aucun salon géré.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TenantGuard {

    private final SalonRepository salonRepository;
    private final SalonMembershipRepository membershipRepository;
    private final com.salahtech.BarberShop_Apis.repositories.BarberRepository barberRepository;

    public boolean isAdmin(ApplicationUser user) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
    }

    /**
     * Ensemble des salons que l'utilisateur peut gérer.
     * Vide pour un ADMIN : utiliser {@link #isAdmin} pour le bypass.
     */
    public Set<Long> manageableSalonIds(ApplicationUser user) {
        if (user == null) return Collections.emptySet();

        // Salons où l'utilisateur a un membership (owner / manager / réceptionniste)
        Set<Long> ids = membershipRepository.findByUser_Id(user.getId()).stream()
                .map(m -> m.getSalon().getId())
                .collect(Collectors.toSet());

        // Compat : salons possédés (SALON_OWNER) même sans membership encore créé
        salonRepository.findByOwner_Id(user.getId()).forEach(s -> ids.add(s.getId()));

        // Compat : salon de rattachement (BARBER)
        Barber barber = user.getBarber();
        if (barber != null && barber.getSalon() != null) {
            ids.add(barber.getSalon().getId());
        }
        return ids;
    }

    // ===== Rôle d'accès par salon (membership) + capacités (matrice) =====

    /** Rôle d'accès de l'utilisateur sur ce salon (membership, ou OWNER si propriétaire direct). */
    public Optional<SalonRole> roleOn(ApplicationUser user, Long salonId) {
        if (user == null || salonId == null) return Optional.empty();
        Optional<SalonRole> fromMembership = membershipRepository
                .findByUser_IdAndSalon_Id(user.getId(), salonId)
                .map(m -> m.getRole());
        if (fromMembership.isPresent()) return fromMembership;
        // Compat : propriétaire direct du salon = OWNER même sans membership encore backfillé.
        boolean owns = salonRepository.findById(salonId)
                .map(s -> s.getOwner() != null && s.getOwner().getId().equals(user.getId()))
                .orElse(false);
        return owns ? Optional.of(SalonRole.OWNER) : Optional.empty();
    }

    private void assertRole(ApplicationUser user, Long salonId, Set<SalonRole> allowed, String action) {
        if (salonId == null) throw new AccessDeniedException("Salon non spécifié");
        if (isAdmin(user)) return;
        SalonRole role = roleOn(user, salonId).orElse(null);
        if (role == null || !allowed.contains(role)) {
            throw new AccessDeniedException("Accès refusé : droits insuffisants (" + action + ")");
        }
    }

    /** Identité salon, horaires, exceptions, services : OWNER + MANAGER. */
    public void assertCanManageSettings(ApplicationUser user, Long salonId) {
        assertRole(user, salonId, EnumSet.of(SalonRole.OWNER, SalonRole.MANAGER), "gestion du salon");
    }

    /** Personnel, postes, affectations : OWNER + MANAGER. */
    public void assertCanManageStaff(ApplicationUser user, Long salonId) {
        assertRole(user, salonId, EnumSet.of(SalonRole.OWNER, SalonRole.MANAGER), "gestion du personnel");
    }

    /** Sous-comptes (manager/réceptionniste) : OWNER uniquement. */
    public void assertCanManageMembers(ApplicationUser user, Long salonId) {
        assertRole(user, salonId, EnumSet.of(SalonRole.OWNER), "gestion des comptes");
    }

    /** Front-desk : file, RDV, clients : OWNER + MANAGER + RECEPTIONIST + EMPLOYEE (prestataire). */
    public void assertCanFrontDesk(ApplicationUser user, Long salonId) {
        assertRole(user, salonId,
                EnumSet.of(SalonRole.OWNER, SalonRole.MANAGER, SalonRole.RECEPTIONIST, SalonRole.EMPLOYEE), "accueil");
    }

    /** True si l'utilisateur est un prestataire (EMPLOYEE) sur ce salon. */
    public boolean isEmployeeOn(ApplicationUser user, Long salonId) {
        return roleOn(user, salonId).map(r -> r == SalonRole.EMPLOYEE).orElse(false);
    }

    /** Actions réservées au propriétaire (suppression salon, facturation). */
    public void assertOwner(ApplicationUser user, Long salonId) {
        assertRole(user, salonId, EnumSet.of(SalonRole.OWNER), "propriétaire");
    }

    public boolean canFrontDesk(ApplicationUser user, Long salonId) {
        if (isAdmin(user)) return true;
        SalonRole role = roleOn(user, salonId).orElse(null);
        return role != null;
    }

    /**
     * Personnel du salon au sens large : membership (tout rôle), propriétaire,
     * ou barbier rattaché (modèle legacy). L'ADMIN passe toujours.
     */
    public boolean isSalonStaff(ApplicationUser user, Long salonId) {
        if (user == null || salonId == null) return false;
        // Requêtes repository uniquement (pas de lazy loading) : utilisable hors
        // session Hibernate, ex. intercepteur STOMP.
        return canFrontDesk(user, salonId)
                || barberRepository.existsByUser_IdAndSalon_Id(user.getId(), salonId);
    }

    /** Lève 403 si l'utilisateur n'est pas du personnel du salon (voir {@link #isSalonStaff}). */
    public void assertSalonStaff(ApplicationUser user, Long salonId) {
        if (!isSalonStaff(user, salonId)) {
            throw new AccessDeniedException("Accès refusé : ce salon n'appartient pas à votre périmètre");
        }
    }

    /**
     * Lève {@link AccessDeniedException} (HTTP 403) si l'utilisateur ne peut
     * pas gérer le salon ciblé. L'ADMIN passe toujours.
     */
    public void assertCanManageSalon(ApplicationUser user, Long salonId) {
        if (salonId == null) {
            throw new AccessDeniedException("Salon non spécifié");
        }
        if (isAdmin(user)) return;
        if (!manageableSalonIds(user).contains(salonId)) {
            throw new AccessDeniedException("Accès refusé : ce salon n'appartient pas à votre périmètre");
        }
    }

    /** True si l'utilisateur a accès au salon (sans lever d'exception). */
    public boolean canManageSalon(ApplicationUser user, Long salonId) {
        if (salonId == null) return false;
        return isAdmin(user) || manageableSalonIds(user).contains(salonId);
    }

    /**
     * Lève 403 si AUCUN des salons fournis n'est dans le périmètre de
     * l'utilisateur. Utile pour les ressources rattachées à plusieurs salons
     * (ex. employé multi-salons). L'ADMIN passe toujours.
     */
    public void assertCanManageAnySalon(ApplicationUser user, Collection<Long> salonIds) {
        if (isAdmin(user)) return;
        Set<Long> mine = manageableSalonIds(user);
        boolean ok = salonIds != null && salonIds.stream()
                .filter(id -> id != null)
                .anyMatch(mine::contains);
        if (!ok) {
            throw new AccessDeniedException("Accès refusé : ressource hors de votre périmètre");
        }
    }
}
