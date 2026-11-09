package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.SalonCatalogDetailDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonCatalogDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonPostDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.CatalogService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonBookingService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Catalogue public côté client : parcourir les salons, leurs services et horaires.
 * Lecture seule, accessible sans rôle particulier (cf. SecurityConfig /v1/catalog/**).
 */
@RestController
@RequestMapping("/v1/catalog")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Catalog", description = "Catalogue client : salons, services, horaires")
public class CatalogController {

    private final CatalogService catalogService;
    private final SalonBookingService bookingService;
    private final SalonPostService postService;

    @Operation(summary = "Lister les salons (catalogue client)")
    @GetMapping("/salons")
    public ResponseEntity<List<SalonCatalogDto>> listSalons() {
        return ResponseEntity.ok(catalogService.listSalons());
    }

    @Operation(summary = "Fiche d'un salon : services actifs + horaires")
    @GetMapping("/salons/{salonId}")
    public ResponseEntity<SalonCatalogDetailDto> getSalon(@PathVariable Long salonId) {
        return ResponseEntity.ok(catalogService.getSalon(salonId));
    }

    @Operation(summary = "Réalisations d'un salon (public)")
    @GetMapping("/salons/{salonId}/posts")
    public ResponseEntity<List<SalonPostDto>> salonPosts(@PathVariable Long salonId) {
        return ResponseEntity.ok(postService.listForSalon(salonId));
    }

    @Operation(summary = "Fil « À la une » : dernières réalisations tous salons")
    @GetMapping("/posts")
    public ResponseEntity<List<SalonPostDto>> feed() {
        return ResponseEntity.ok(postService.feed());
    }

    @Operation(summary = "Créneaux disponibles pour un salon + service + date")
    @GetMapping("/salons/{salonId}/slots")
    public ResponseEntity<List<String>> slots(
            @PathVariable Long salonId,
            @RequestParam Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.availableSlots(salonId, serviceId, date));
    }
}
