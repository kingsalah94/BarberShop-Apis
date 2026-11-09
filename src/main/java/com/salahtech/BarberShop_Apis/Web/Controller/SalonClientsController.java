package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.SalonClientDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonBookingService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Clients d'un salon (agrégés depuis les réservations). Lecture = tout membre (front-desk) ;
 * un prestataire ne voit que les clients qu'il a servis (données de contact incluses).
 */
@RestController
@RequestMapping("/v1/salons/{salonId}/clients")
@RequiredArgsConstructor
@Tag(name = "Salon Clients", description = "Clients d'un salon")
public class SalonClientsController {

    private final SalonBookingService bookingService;
    private final TenantGuard tenantGuard;
    private final com.salahtech.BarberShop_Apis.repositories.EmployeeRepository employeeRepository;

    @Operation(summary = "Lister les clients du salon")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalonClientDto>> list(@PathVariable Long salonId,
                                                     @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        Long scope = null;
        if (tenantGuard.isEmployeeOn(user, salonId)) {
            scope = employeeRepository.findByUser_Id(user.getId()).map(e -> e.getId()).orElse(-1L);
        }
        return ResponseEntity.ok(bookingService.listClients(salonId, scope));
    }
}
