package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.BookingCreateDto;
import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BookingService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
@Tag(name = "Bookings", description = "Gestion des réservations")
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository; // Gardé uniquement pour la recherche complexe (Pageable)

    // ========================================================================
    // 📝 CRÉATION & LECTURE SIMPLE
    // ========================================================================

    @Operation(summary = "Créer une réservation (Client)")
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CLIENT')") // Sécurisé
    public ResponseEntity<BookingDto> create(@Valid @RequestBody BookingCreateDto dto) {
        // Le service récupère l'utilisateur connecté via SecurityContext,
        // ou vous pouvez passer user.getId() si vous modifiez le service.
        BookingDto saved = bookingService.createBooking(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Trouver une réservation par ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Tout utilisateur connecté peut essayer, le service vérifiera s'il a le droit
    public ResponseEntity<BookingDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.findById(id));
    }

    // ========================================================================
    // 🔍 LISTES & RECHERCHES
    // ========================================================================

    @Operation(summary = "Mes réservations (Client connecté)")
    @GetMapping("/my-bookings")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ResponseEntity<List<BookingDto>> getMyBookings(@CurrentUser ApplicationUser user) {
        if (user.getClient() == null)
            return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(bookingService.findByClientId(user.getClient().getId()));
    }

    @Operation(summary = "Réservations d'un barbier (Barbier connecté ou Admin)")
    @GetMapping("/barber/my-bookings")
    @PreAuthorize("hasAuthority('ROLE_BARBER')")
    public ResponseEntity<List<BookingDto>> getBarberBookings(@CurrentUser ApplicationUser user) {
        // Supposons que le Barber est lié au User.
        // Note: Il faudrait idéalement une méthode findByBarberUserId dans le service
        // Ici on utilise l'ID direct si le front l'envoie, sinon on adapte le service.
        // Pour l'instant, utilisons l'endpoint public filtré :
        return ResponseEntity.ok(bookingService.findByBarberId(user.getId()));
    }

    @Operation(summary = "Réservations par Barbier (Public/Admin)")
    @GetMapping("/by-barber/{barberId}")
    public ResponseEntity<List<BookingDto>> findByBarberId(@PathVariable @NotNull Long barberId) {
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
            Pageable pageable) {

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
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(bookingService.findByBarberIdBetweenDates(barberId, start, end));
    }

    // ========================================================================
    // ⚙️ ACTIONS MÉTIER (CHANGEMENT D'ÉTAT)
    // ========================================================================

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('ROLE_BARBER')")
    @Operation(summary = "Terminer le RDV et Encaisser (Cash)")
    public ResponseEntity<BookingDto> complete(@PathVariable Long id, @CurrentUser ApplicationUser user) {
        // user.getId() permet de vérifier que c'est bien CE barbier qui termine SON rdv
        return ResponseEntity.ok(bookingService.completeBooking(id, user.getId()));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('ROLE_BARBER')")
    @Operation(summary = "Confirmer un rendez-vous (Barbier)")
    public ResponseEntity<BookingDto> confirm(@PathVariable Long id, @CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, user.getId()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Annuler un rendez-vous (Client ou Barbier)")
    public ResponseEntity<BookingDto> cancel(@PathVariable Long id, @CurrentUser ApplicationUser user) {
        // Le service vérifie si 'user' est le propriétaire ou le barbier
        return ResponseEntity.ok(bookingService.cancelBooking(id, user.getId()));
    }
}