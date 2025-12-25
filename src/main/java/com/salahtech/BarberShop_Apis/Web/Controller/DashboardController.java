package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.DashboardStatsDto;
import com.salahtech.BarberShop_Apis.Dtos.TimePointDecimalDto;
import com.salahtech.BarberShop_Apis.Dtos.TimePointLongDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Services.Interfaces.DashboardService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Dashboard", description = "Statistiques et Graphiques")
public class DashboardController {

    private final DashboardService service;

    @Operation(summary = "Statistiques Globales (Admin) ou Personnelles (Barber)")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER')")
    public ResponseEntity<DashboardStatsDto> getStats(@CurrentUser ApplicationUser user) {
        Long barberId = determineTargetId(user);

        if (barberId == null) {
            // Admin voit tout
            return ResponseEntity.ok(service.getAllStatsForAdmin());
        } else {
            // Barber voit ses stats
            return ResponseEntity.ok(service.getStatsByBarber(barberId));
        }
    }

    @Operation(summary = "Tendance des Réservations (Courbe)")
    @GetMapping("/trends/bookings")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER')")
    public ResponseEntity<List<TimePointLongDto>> getBookingsTrend(
            @CurrentUser ApplicationUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Long barberId = determineTargetId(user);
        return ResponseEntity.ok(service.getBookingsTrend(barberId, from, to));
    }

    @Operation(summary = "Tendance du Chiffre d'Affaires (Courbe)")
    @GetMapping("/trends/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER')")
    public ResponseEntity<List<TimePointDecimalDto>> getRevenueTrend(
            @CurrentUser ApplicationUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Long barberId = determineTargetId(user);
        return ResponseEntity.ok(service.getRevenueTrend(barberId, from, to));
    }

    @Operation(summary = "Répartition par Statut (Camembert)")
    @GetMapping("/breakdown/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER')")
    public ResponseEntity<Map<BookingStatus, Long>> getStatusBreakdown(
            @CurrentUser ApplicationUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Long barberId = determineTargetId(user);
        return ResponseEntity.ok(service.getStatusBreakdown(barberId, from, to));
    }

    // --- Helper Privé ---
    private Long determineTargetId(ApplicationUser user) {
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
        // Si Admin -> null (veut dire "global" pour le service)
        // Si Barber -> renvoie son ID
        return isAdmin ? null : user.getId();
    }
}