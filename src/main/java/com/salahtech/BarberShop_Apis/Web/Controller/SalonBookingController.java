package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.CreateSalonBookingRequest;
import com.salahtech.BarberShop_Apis.Dtos.SalonBookingDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonStatsDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonBookingService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.repositories.EmployeeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Réservations orientées salon.
 * Création = client connecté ; consultation/confirmation/annulation = staff front-desk.
 */
@RestController
@RequestMapping("/v1/salons/{salonId}/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Salon Bookings", description = "Réservations orientées salon")
public class SalonBookingController {

    private final SalonBookingService bookingService;
    private final TenantGuard tenantGuard;
    private final EmployeeRepository employeeRepository;

    /**
     * Si l'utilisateur est un prestataire (EMPLOYEE) sur ce salon, renvoie l'id de
     * son profil Employee pour limiter la vue à ses propres RDV ; sinon null (vue salon).
     */
    private Long scopeEmployeeId(ApplicationUser user, Long salonId) {
        if (!tenantGuard.isEmployeeOn(user, salonId)) return null;
        return employeeRepository.findByUser_Id(user.getId()).map(e -> e.getId()).orElse(null);
    }

    @Operation(summary = "Réserver (client) : salon + service + créneau")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonBookingDto> create(@PathVariable Long salonId,
                                                  @Valid @RequestBody CreateSalonBookingRequest req,
                                                  @CurrentUser ApplicationUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(salonId, req, user));
    }

    @Operation(summary = "Lister les réservations du salon pour une date (staff)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalonBookingDto>> list(
            @PathVariable Long salonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        // Prestataire connecté → uniquement ses RDV ; owner/manager/réceptionniste → tout le salon.
        return ResponseEntity.ok(bookingService.listForSalon(salonId, date, scopeEmployeeId(user, salonId)));
    }

    @Operation(summary = "Confirmer une réservation (+ poste / prestataire optionnels)")
    @PostMapping("/{bookingId}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonBookingDto> confirm(@PathVariable Long salonId,
                                                   @PathVariable Long bookingId,
                                                   @RequestParam(required = false) Long workstationId,
                                                   @RequestParam(required = false) Long employeeId,
                                                   @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        return ResponseEntity.ok(bookingService.confirm(salonId, bookingId, workstationId, employeeId));
    }

    @Operation(summary = "Affecter / réaffecter un prestataire à une réservation")
    @PostMapping("/{bookingId}/assign")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonBookingDto> assign(@PathVariable Long salonId,
                                                  @PathVariable Long bookingId,
                                                  @RequestParam(required = false) Long employeeId,
                                                  @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        return ResponseEntity.ok(bookingService.assignEmployee(salonId, bookingId, employeeId));
    }

    @Operation(summary = "Client arrivé : check-in → ajoute à la file d'attente")
    @PostMapping("/{bookingId}/checkin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonBookingDto> checkIn(@PathVariable Long salonId,
                                                   @PathVariable Long bookingId,
                                                   @RequestParam(required = false) Long workstationId,
                                                   @RequestParam(required = false) Long employeeId,
                                                   @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        return ResponseEntity.ok(bookingService.checkIn(salonId, bookingId, workstationId, employeeId));
    }

    @Operation(summary = "Annuler une réservation (staff)")
    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonBookingDto> cancel(@PathVariable Long salonId,
                                                  @PathVariable Long bookingId,
                                                  @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        return ResponseEntity.ok(bookingService.cancel(salonId, bookingId, user));
    }

    @Operation(summary = "Statistiques des réservations du salon sur une période")
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonStatsDto> stats(
            @PathVariable Long salonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        return ResponseEntity.ok(bookingService.getStats(salonId, from, to, scopeEmployeeId(user, salonId)));
    }
}
