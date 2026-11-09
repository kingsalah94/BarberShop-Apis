package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.DashboardStatsDto;
import com.salahtech.BarberShop_Apis.Dtos.TimePointDecimalDto;
import com.salahtech.BarberShop_Apis.Dtos.TimePointLongDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Services.Interfaces.DashboardService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.repositories.BarberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Statistiques et Graphiques")
public class DashboardController {

    private final DashboardService service;
    private final com.salahtech.BarberShop_Apis.Services.Interfaces.SalonBookingService salonBookingService;

    @Operation(summary = "Prochaines réservations, tous salons (ADMIN)")
    @GetMapping("/upcoming")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.salahtech.BarberShop_Apis.Dtos.SalonBookingDto>> upcoming() {
        return ResponseEntity.ok(salonBookingService.upcomingPlatform());
    }

    // Tableau de bord PLATEFORME (console admin) — réservations du modèle salon.
    // Les vues par salon / par prestataire passent par /v1/salons/{id}/bookings/stats.

    @Operation(summary = "Statistiques globales de la plateforme (ADMIN)")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardStatsDto> getStats() {
        return ResponseEntity.ok(service.getAllStatsForAdmin());
    }

    @Operation(summary = "Tendance des réservations (ADMIN)")
    @GetMapping("/trends/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TimePointLongDto>> getBookingsTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.getBookingsTrend(from, to));
    }

    @Operation(summary = "Tendance du chiffre d'affaires (ADMIN)")
    @GetMapping("/trends/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TimePointDecimalDto>> getRevenueTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.getAllRevenueTrend(from, to));
    }

    @Operation(summary = "Répartition par statut (ADMIN)")
    @GetMapping("/breakdown/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<BookingStatus, Long>> getStatusBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.getAllStatusBreakdown(from, to));
    }
}