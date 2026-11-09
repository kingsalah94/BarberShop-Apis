package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.SalonServiceDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonServiceRequest;
import com.salahtech.BarberShop_Apis.Services.Implementations.FileStorageService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonOfferingService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Catalogue de services d'un salon.
 * Lecture = tout membre (front-desk) ; gestion = OWNER + MANAGER (paramètres).
 */
@RestController
@RequestMapping("/v1/salons/{salonId}/services")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Salon Services", description = "Catalogue de services d'un salon")
public class SalonServiceController {

    private final SalonOfferingService offeringService;
    private final FileStorageService fileStorageService;
    private final TenantGuard tenantGuard;

    @Operation(summary = "Uploader une image de service → renvoie son URL")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadImage(@PathVariable Long salonId,
                                                           @RequestParam("file") MultipartFile file,
                                                           @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageSettings(user, salonId);
        return ResponseEntity.ok(Map.of("imageUrl", fileStorageService.storeFile(file)));
    }

    @Operation(summary = "Lister les services du salon")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalonServiceDto>> list(
            @PathVariable Long salonId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        return ResponseEntity.ok(offeringService.list(salonId, activeOnly));
    }

    @Operation(summary = "Créer un service")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonServiceDto> create(@PathVariable Long salonId,
                                                  @Valid @RequestBody SalonServiceRequest req,
                                                  @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageSettings(user, salonId);
        return ResponseEntity.status(HttpStatus.CREATED).body(offeringService.create(salonId, req));
    }

    @Operation(summary = "Mettre à jour un service")
    @PutMapping("/{serviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonServiceDto> update(@PathVariable Long salonId,
                                                  @PathVariable Long serviceId,
                                                  @RequestBody SalonServiceRequest req,
                                                  @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageSettings(user, salonId);
        return ResponseEntity.ok(offeringService.update(salonId, serviceId, req));
    }

    @Operation(summary = "Activer / désactiver un service")
    @PatchMapping("/{serviceId}/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonServiceDto> setActive(@PathVariable Long salonId,
                                                     @PathVariable Long serviceId,
                                                     @RequestParam boolean active,
                                                     @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageSettings(user, salonId);
        return ResponseEntity.ok(offeringService.setActive(salonId, serviceId, active));
    }

    @Operation(summary = "Supprimer un service")
    @DeleteMapping("/{serviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long salonId,
                                       @PathVariable Long serviceId,
                                       @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageSettings(user, salonId);
        offeringService.delete(salonId, serviceId);
        return ResponseEntity.noContent().build();
    }
}
