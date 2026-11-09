package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.EffectiveHoursDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonExceptionDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonHoursDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonHoursService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Horaires d'ouverture d'un salon (semaine type + exceptions).
 * Lecture = tout membre (front-desk) ; modification = OWNER + MANAGER (paramètres).
 */
@RestController
@RequestMapping("/v1/salons/{salonId}")
@RequiredArgsConstructor
@Tag(name = "Salon Hours", description = "Horaires d'ouverture et exceptions d'un salon")
public class SalonHoursController {

    private final SalonHoursService hoursService;
    private final TenantGuard tenantGuard;

    @Operation(summary = "Semaine type du salon (7 jours)")
    @GetMapping("/hours")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalonHoursDto>> getWeekly(@PathVariable Long salonId,
                                                         @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        return ResponseEntity.ok(hoursService.getWeeklyHours(salonId));
    }

    @Operation(summary = "Mettre à jour la semaine type")
    @PutMapping("/hours")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalonHoursDto>> updateWeekly(@PathVariable Long salonId,
                                                            @RequestBody List<SalonHoursDto> hours,
                                                            @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageSettings(user, salonId);
        return ResponseEntity.ok(hoursService.updateWeeklyHours(salonId, hours));
    }

    @Operation(summary = "Lister les exceptions (fermetures / horaires spéciaux)")
    @GetMapping("/exceptions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalonExceptionDto>> listExceptions(
            @PathVariable Long salonId,
            @RequestParam(required = false, defaultValue = "true") boolean upcoming,
            @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        return ResponseEntity.ok(hoursService.listExceptions(salonId, upcoming));
    }

    @Operation(summary = "Créer / remplacer une exception pour une date")
    @PostMapping("/exceptions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonExceptionDto> createException(@PathVariable Long salonId,
                                                             @RequestBody SalonExceptionDto req,
                                                             @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageSettings(user, salonId);
        return ResponseEntity.status(HttpStatus.CREATED).body(hoursService.createException(salonId, req));
    }

    @Operation(summary = "Supprimer une exception")
    @DeleteMapping("/exceptions/{exceptionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteException(@PathVariable Long salonId,
                                                @PathVariable Long exceptionId,
                                                @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageSettings(user, salonId);
        hoursService.deleteException(salonId, exceptionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Horaires effectifs résolus pour une date")
    @GetMapping("/hours/effective")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EffectiveHoursDto> effective(
            @PathVariable Long salonId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        return ResponseEntity.ok(hoursService.getEffectiveHours(salonId, date));
    }
}
