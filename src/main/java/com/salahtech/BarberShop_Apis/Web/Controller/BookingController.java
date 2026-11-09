package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.BookingCreateDto;
import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BookingService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.repositories.BookingRepository;
import com.salahtech.BarberShop_Apis.repositories.BarberRepository;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import org.springframework.security.access.AccessDeniedException;
import java.util.Objects;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/bookings")
@RequiredArgsConstructor
@Validated
@Tag(name = "Bookings", description = "Gestion des réservations")
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository; // Gardé uniquement pour la recherche complexe (Pageable)
    private final BarberRepository barberRepository;
    private final TenantGuard tenantGuard;

    // ========================================================================
    // 🔒 CONTRÔLES D'ACCÈS (anti-IDOR / multi-tenant)
    // ========================================================================

    /** Id du profil barbier de l'utilisateur (null s'il n'en a pas). */
    private Long myBarberId(ApplicationUser user) {
        return barberRepository.findByUser_Id(user.getId()).map(b -> b.getId()).orElse(null);
    }

    private static Long myClientId(ApplicationUser user) {
        return user.getClient() != null ? user.getClient().getId() : null;
    }

    /** ADMIN, client du RDV, barbier du RDV ou personnel du salon du RDV. */
    private void assertCanAccessBooking(ApplicationUser user, Long bookingId) {
        if (tenantGuard.isAdmin(user)) return;
        Long uid = user.getId();
        if (Objects.equals(uid, bookingRepository.findClientUserIdById(bookingId).orElse(null))) return;
        if (Objects.equals(uid, bookingRepository.findBarberUserIdById(bookingId).orElse(null))) return;
        Long salonId = bookingRepository.findSalonIdById(bookingId).orElse(null);
        if (salonId != null && tenantGuard.isSalonStaff(user, salonId)) return;
        throw new AccessDeniedException("Accès refusé : ce rendez-vous ne vous concerne pas");
    }

    /** ADMIN, le barbier lui-même ou le personnel de son salon. */
    private void assertCanAccessBarber(ApplicationUser user, Long barberId) {
        if (tenantGuard.isAdmin(user)) return;
        if (Objects.equals(barberId, myBarberId(user))) return;
        Long salonId = barberRepository.findSalonIdById(barberId).orElse(null);
        if (salonId != null && tenantGuard.isSalonStaff(user, salonId)) return;
        throw new AccessDeniedException("Accès refusé : planning d'un autre barbier");
    }

    // ========================================================================
    // 📝 CRÉATION & LECTURE SIMPLE
    // ========================================================================

    @Operation(summary = "Créer une réservation (Client)")
    @PostMapping
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')") // Sécurisé
    public ResponseEntity<BookingDto> create(@Valid @RequestBody BookingCreateDto dto,
            @CurrentUser ApplicationUser user) {
        // Le service récupère l'utilisateur connecté via SecurityContext,
        // ou vous pouvez passer user.getId() si vous modifiez le service.
        // 1. Vérifier le rôle de celui qui fait la demande
        boolean isStaff = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN")
                        || r.getName().equals("BARBER")
                        || r.getName().equals("SALON_OWNER"));

        if (isStaff) {
            // 👮‍♂️ Cas ADMIN / STAFF :
            // On fait confiance à l'ID envoyé dans le DTO
            if (dto.getClientId() == null) {
                // Si l'admin a oublié de choisir un client
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "L'ID du client est obligatoire pour une réservation Admin.");
            }
            // On laisse le DTO tel quel, le service utilisera dto.getClientId()

        } else {
            // 👤 Cas CLIENT LAMBDA :
            // On ignore ce qu'il a envoyé et on FORCE son propre ID (Sécurité)
            if (user.getClient() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cet utilisateur n'a pas de profil Client associé.");
            }
            dto.setClientId(user.getClient().getId());
        }
        BookingDto saved = bookingService.createBooking(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ========================================================================
    // ✏️ MISE À JOUR (UPDATE)
    // ========================================================================

    @Operation(summary = "Mettre à jour une réservation")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // ✅ CORRECTION : On laisse entrer tout le monde, le code filtre après
    public ResponseEntity<BookingDto> update(
            @PathVariable Long id,
            @Valid @RequestBody BookingDto dto,
            @CurrentUser ApplicationUser user) {

        assertCanAccessBooking(user, id);

        // Vérification du rôle
        boolean isStaff = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN")
                        || r.getName().equals("BARBER")
                        || r.getName().equals("SALON_OWNER"));

        if (isStaff) {
            // 👮‍♂️ Cas ADMIN / STAFF :
            // On fait confiance à l'ID envoyé dans le DTO pour changer le client
            if (dto.getClientId() == null) {
                // On pourrait laisser passer si on ne veut pas changer le client,
                // mais si c'est une création déguisée ou un changement obligatoire :
                // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ID du client est
                // requis.");
            }
            // On ne touche à rien, le Service utilisera dto.getClientId() tel quel.

        } else {
            // 👤 Cas CLIENT LAMBDA :
            // SÉCURITÉ : On vérifie d'abord que le RDV lui appartient déjà (Anti-Vol)
            // (Idéalement cette vérif se fait ici ou dans le service via @PreAuthorize plus
            // fin)

            if (user.getClient() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cet utilisateur n'a pas de profil Client associé.");
            }

            // On FORCE l'ID du client connecté pour empêcher qu'il ne modifie le RDV de
            // quelqu'un d'autre
            // ou qu'il ne s'attribue le RDV d'un autre.
            dto.setClientId(user.getClient().getId());

            // Note: Le service devra vérifier que le booking 'id' appartient bien à ce
            // 'clientId'
            // avant de faire l'update, sinon un client pourrait modifier le RDV d'un autre
            // en devinant l'ID.
        }

        // Appel du service (qui contient maintenant la logique de changement de client)
        BookingDto updated = bookingService.updateBooking(id, dto);

        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Trouver une réservation par ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Tout utilisateur connecté peut essayer, le service vérifiera s'il a le droit
    public ResponseEntity<BookingDto> findById(@PathVariable Long id, @CurrentUser ApplicationUser user) {
        assertCanAccessBooking(user, id);
        return ResponseEntity.ok(bookingService.findById(id));
    }

    // ========================================================================
    // 🔍 LISTES & RECHERCHES
    // ========================================================================

    @Operation(summary = "Mes réservations (Client connecté)")
    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    public ResponseEntity<List<BookingDto>> getMyBookings(@CurrentUser ApplicationUser user) {
        if (user.getClient() == null)
            return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(bookingService.findByClientId(user.getClient().getId()));
    }

    @Operation(summary = "Réservations d'un barbier (Barbier connecté ou Admin)")
    @GetMapping("/barber/my-bookings")
    @PreAuthorize("hasRole('BARBER') or hasRole('ADMIN')")
    public ResponseEntity<List<BookingDto>> getBarberBookings(@CurrentUser ApplicationUser user) {
        // Supposons que le Barber est lié au User.
        // Note: Il faudrait idéalement une méthode findByBarberUserId dans le service
        // Ici on utilise l'ID direct si le front l'envoie, sinon on adapte le service.
        // Pour l'instant, utilisons l'endpoint public filtré :
        Long barberId = myBarberId(user);
        if (barberId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(bookingService.findByBarberId(barberId));
    }

    @GetMapping("/by-client/{clientId}")
    @Operation(summary = "Historique des RDV d'un client spécifique")
    @PreAuthorize("hasAnyRole('BARBER', 'ADMIN')")
    public ResponseEntity<List<BookingDto>> getByClientId(@PathVariable Long clientId,
                                                          @CurrentUser ApplicationUser user) {
        List<BookingDto> all = bookingService.findByClientId(clientId);
        if (tenantGuard.isAdmin(user)) return ResponseEntity.ok(all);
        // Un barbier ne voit que l'historique du client AVEC lui.
        Long barberId = myBarberId(user);
        return ResponseEntity.ok(all.stream()
                .filter(b -> barberId != null && barberId.equals(b.getBarberId()))
                .toList());
    }

    @Operation(summary = "Réservations par Barbier (Public/Admin)")
    @GetMapping("/by-barber/{barberId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookingDto>> findByBarberId(@PathVariable @NotNull Long barberId,
                                                           @CurrentUser ApplicationUser user) {
        assertCanAccessBarber(user, barberId);
        return ResponseEntity.ok(bookingService.findByBarberId(barberId));
    }

    @Operation(summary = "Recherche avancée avec filtres")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<BookingDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long barberId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable,
            @CurrentUser ApplicationUser user) {

        // Périmètre : l'ADMIN voit tout ; un barbier ses RDV ; un client les siens.
        if (!tenantGuard.isAdmin(user)) {
            Long myBarber = myBarberId(user);
            if (myBarber != null) {
                barberId = myBarber;
            } else if (myClientId(user) != null) {
                clientId = myClientId(user);
            } else {
                throw new AccessDeniedException("Accès refusé");
            }
        }

        BookingStatus st = (status == null || status.isBlank()) ? null : BookingStatus.valueOf(status.toUpperCase());

        // Gestion des dates par défaut
        LocalDateTime from = (dateFrom != null) ? dateFrom.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime to = (dateTo != null) ? dateTo.plusDays(1).atStartOfDay()
                : LocalDateTime.of(9999, 12, 31, 23, 59);
        String qEff = (q == null || q.isBlank()) ? null : q.trim();

        // Note: Idéalement, déplacez 'searchWithFilters' dans le Service pour ne pas
        // injecter le Repository ici.
        return bookingRepository.searchWithFilters(qEff, st, barberId, clientId, from, to, pageable)
                .map(BookingDto::fromEntity);
    }

    @Operation(summary = "Compter les réservations complétées d'un barbier")
    @GetMapping("/barber/{barberId}/completed/count")
    public ResponseEntity<Map<String, Long>> countCompletedByBarber(@PathVariable @NotNull Long barberId) {
        Long count = bookingService.countCompletedByBarber(barberId);
        return ResponseEntity.ok(Map.of("barberId", barberId, "completedCount", count));
    }

    @Operation(summary = "Vérifier les conflits (Planning)")
    @GetMapping("/barber/{barberId}/between")
    public ResponseEntity<List<BookingDto>> findByBarberBetweenDates(
            @PathVariable @NotNull Long barberId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @CurrentUser ApplicationUser user) {
        assertCanAccessBarber(user, barberId);
        return ResponseEntity.ok(bookingService.findByBarberIdBetweenDates(barberId, start, end));
    }

    // ========================================================================
    // ⚙️ ACTIONS MÉTIER (CHANGEMENT D'ÉTAT)
    // ========================================================================

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('BARBER') or hasRole('ADMIN')")
    @Operation(summary = "Terminer le RDV et Encaisser (Cash)")
    public ResponseEntity<BookingDto> complete(@PathVariable Long id, @CurrentUser ApplicationUser user) {
        assertCanAccessBooking(user, id);
        // user.getId() permet de vérifier que c'est bien CE barbier qui termine SON rdv
        return ResponseEntity.ok(bookingService.completeBooking(id, user.getId()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('BARBER') or hasRole('ADMIN')")
    @Operation(summary = "Confirmer un rendez-vous (Barbier)")
    public ResponseEntity<BookingDto> confirm(@PathVariable Long id, @CurrentUser ApplicationUser user) {
        assertCanAccessBooking(user, id);
        return ResponseEntity.ok(bookingService.confirmBooking(id, user.getId()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Annuler un rendez-vous (Client ou Barbier)")
    public ResponseEntity<BookingDto> cancel(@PathVariable Long id, @CurrentUser ApplicationUser user) {
        // Le service vérifie si 'user' est le propriétaire ou le barbier
        return ResponseEntity.ok(bookingService.cancelBooking(id, user.getId()));
    }
}