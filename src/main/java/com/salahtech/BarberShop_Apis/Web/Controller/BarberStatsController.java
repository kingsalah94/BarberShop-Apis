package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.BarberStatsDto;
import com.salahtech.BarberShop_Apis.Services.BarberStatsService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/barbers")
@RequiredArgsConstructor
@Tag(name = "Barber Statistics", description = "Statistiques et Analytics")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BarberStatsController {

    private final BarberStatsService barberStatsService;

    @Operation(summary = "Voir les stats d'un barbier (Admin ou le barbier lui-même)")
    @GetMapping("/{barberId}/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BarberStatsDto> getBarberStats(
            @PathVariable Long barberId,
            @CurrentUser ApplicationUser user) {

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));

        // Si pas Admin, on vérifie que le user est bien le propriétaire du profil Barbier
        if (!isAdmin) {
            Barber currentBarber = user.getBarber();
            if (currentBarber == null || !currentBarber.getId().equals(barberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(barberStatsService.getBarberStats(barberId));
    }

    @Operation(summary = "Voir MES stats (Barbier connecté)")
    @GetMapping("/me/stats")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<BarberStatsDto> getCurrentBarberStats(@CurrentUser ApplicationUser user) {
        
        Barber barber = user.getBarber();
        if (barber == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(barberStatsService.getBarberStats(barber.getId()));
    }
}