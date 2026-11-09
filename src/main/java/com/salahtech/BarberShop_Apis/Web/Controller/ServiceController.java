package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.ServiceDto;
import com.salahtech.BarberShop_Apis.Dtos.ServiceStatsDto;
import com.salahtech.BarberShop_Apis.Dtos.StatusUpdateRequest;
import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;
import com.salahtech.BarberShop_Apis.Services.Implementations.BarberServiceServiceImpl;
import com.salahtech.BarberShop_Apis.Services.Implementations.FileStorageService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ServiceService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/services") // ✅ CHANGEMENT ICI : /v1/services → /api/v1/services
@RequiredArgsConstructor
@Validated
@Tag(name = "Services", description = "Gestion des prestations proposées par les barbiers/salons")
public class ServiceController {

    private final ServiceService serviceService;
    private final FileStorageService fileStorageService;
    private final com.salahtech.BarberShop_Apis.repositories.BarberServiceRepository barberServiceRepository;
    private final com.salahtech.BarberShop_Apis.repositories.BarberRepository barberRepository;
    private final com.salahtech.BarberShop_Apis.Config.TenantGuard tenantGuard;

    private static final String MANAGER_ROLES = "hasAnyRole('BARBER', 'ADMIN', 'SALON_OWNER')";

    /** ADMIN, le barbier propriétaire, ou le personnel du salon du barbier. */
    private void assertCanManageBarber(ApplicationUser user, Long barberId) {
        if (tenantGuard.isAdmin(user)) return;
        if (barberId == null) throw new org.springframework.security.access.AccessDeniedException("Barbier non spécifié");
        Long mine = barberRepository.findByUser_Id(user.getId()).map(b -> b.getId()).orElse(null);
        if (barberId.equals(mine)) return;
        Long salonId = barberRepository.findSalonIdById(barberId).orElse(null);
        if (salonId != null && tenantGuard.isSalonStaff(user, salonId)) return;
        throw new org.springframework.security.access.AccessDeniedException("Accès refusé : prestation d'un autre barbier");
    }

    private void assertCanManageService(ApplicationUser user, Long serviceId) {
        if (tenantGuard.isAdmin(user)) return;
        Long barberId = barberServiceRepository.findBarberIdById(serviceId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Prestation sans barbier"));
        assertCanManageBarber(user, barberId);
    }

    // ======= CRUD =======

    @Operation(summary = "Créer une prestation", responses = @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = ServiceDto.class))))
    @PostMapping
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<ServiceDto> create(@Valid @RequestBody ServiceDto dto, @CurrentUser ApplicationUser user) {
        assertCanManageBarber(user, dto.getBarberId());
        ServiceDto saved = serviceService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Créer une prestation avec image.
     * IMPORTANT : Dans Postman, choisir Body -> form-data
     * Clé "service" (Type Text/JSON) : { "name": "Coupe", "price": 10, "duration":
     * 30 }
     * Clé "file" (Type File) : Choisir l'image
     */
    /**
     * Créer une prestation avec image (Support Multipart).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('BARBER', 'ADMIN', 'SALON_OWNER')")
    public ResponseEntity<ServiceDto> createService(
            @CurrentUser ApplicationUser user,
            @RequestPart("service") @Valid ServiceDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        // Gestion de l'image
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = fileStorageService.storeFile(file);
        }

        // ✅ CORRECTION : On ne calcule pas l'ID ici.
        // On passe 'user' et 'dto' au service. Il se débrouillera.
        ServiceDto created = serviceService.create(dto, imageUrl, user);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Mettre à jour une prestation par ID")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('BARBER') or hasRole('ADMIN')")
    public ResponseEntity<ServiceDto> update(
            @CurrentUser ApplicationUser user,
            @PathVariable Long id,
            @RequestPart("service") @Valid ServiceDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        assertCanManageService(user, id);
        String newImageUrl = null;

        // 1. Upload de la NOUVELLE image (si présente)
        if (file != null && !file.isEmpty()) {
            newImageUrl = fileStorageService.storeFile(file);
        }

        // 2. Appel du service métier update
        // On passe l'ID de la prestation, le DTO, et la potentielle nouvelle URL
        ServiceDto updated = serviceService.update(id, user.getId(), dto, newImageUrl);

        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<Page<ServiceDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long barberId,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(serviceService.search(q, category, active, barberId, pageable));
    }

    @Operation(summary = "Trouver une prestation par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.findById(id));
    }

    @Operation(summary = "Supprimer une prestation par ID", responses = {
            @ApiResponse(responseCode = "204", description = "Supprimé")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<Void> delete(@PathVariable Long id, @CurrentUser ApplicationUser user) {
        assertCanManageService(user, id);
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ======= LECTURE LISTES =======

    @Operation(summary = "Lister les prestations d'un barbier")
    @GetMapping("/by-barber/{barberId}")
    public ResponseEntity<List<ServiceDto>> findByBarberId(@PathVariable @NotNull Long barberId) {
        return ResponseEntity.ok(serviceService.findByBarberId(barberId));
    }

    @Operation(summary = "Lister les prestations par catégorie")
    @GetMapping("/by-category")
    public ResponseEntity<List<ServiceDto>> findByCategory(
            @RequestParam @NotNull @Parameter(description = "Catégorie de service") ServiceCategory category) {
        return ResponseEntity.ok(serviceService.findByCategory(category));
    }

    @Operation(summary = "Lister les prestations actives/inactives")
    @GetMapping("/active")
    public ResponseEntity<List<ServiceDto>> findByActive(@RequestParam @NotNull Boolean active) {
        return ResponseEntity.ok(serviceService.findByActive(active));
    }

    @Operation(summary = "Prestations actives d'un barbier")
    @GetMapping("/by-barber/{barberId}/active")
    public ResponseEntity<List<ServiceDto>> findActiveServicesByBarberId(@PathVariable @NotNull Long barberId) {
        return ResponseEntity.ok(serviceService.findActiveServicesByBarberId(barberId));
    }

    @Operation(summary = "Prestations actives par catégorie, triées prix croissant")
    @GetMapping("/active/by-category/price-asc")
    public ResponseEntity<List<ServiceDto>> findActiveByCategoryOrderByPriceAsc(
            @RequestParam @NotNull ServiceCategory category) {
        return ResponseEntity.ok(serviceService.findActiveByCategoryOrderByPriceAsc(category));
    }

    // ======= RECHERCHES =======

    @Operation(summary = "Recherche par fourchette de prix (actives uniquement)")
    @GetMapping("/search/price-range")
    public ResponseEntity<List<ServiceDto>> findByPriceRange(
            @RequestParam @NotNull @DecimalMin("0.0") BigDecimal minPrice,
            @RequestParam @NotNull @DecimalMin("0.0") BigDecimal maxPrice) {
        return ResponseEntity.ok(serviceService.findByPriceRange(minPrice, maxPrice));
    }

    @Operation(summary = "Recherche par durée maximale (actives uniquement)")
    @GetMapping("/search/max-duration")
    public ResponseEntity<List<ServiceDto>> findByMaxDuration(
            @RequestParam @NotNull @Min(1) Integer maxDuration) {
        return ResponseEntity.ok(serviceService.findByMaxDuration(maxDuration));
    }

    @Operation(summary = "Recherche par nom (contient, actives uniquement)")
    @GetMapping("/search/by-name")
    public ResponseEntity<List<ServiceDto>> findByNameContaining(
            @RequestParam @NotBlank String q) {
        return ResponseEntity.ok(serviceService.findByNameContaining(q));
    }

    // ======= METADONNÉES =======

    @Operation(summary = "Catégories actives distinctes")
    @GetMapping("/categories/active")
    public ResponseEntity<List<ServiceCategory>> getDistinctActiveCategories() {
        return ResponseEntity.ok(serviceService.getDistinctActiveCategories());
    }

    // ======= OPÉRATIONS SPÉCIALES =======

    @Operation(summary = "Activer/désactiver un service", responses = {
            @ApiResponse(responseCode = "200", description = "Statut modifié avec succès", content = @Content(schema = @Schema(implementation = ServiceDto.class)))
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<ServiceDto> toggleStatus(
            @PathVariable Long id,
            @RequestBody @Valid StatusUpdateRequest request,
            @CurrentUser ApplicationUser user) {
        assertCanManageService(user, id);
        ServiceDto updated = serviceService.toggleServiceStatus(id, request.getActive());
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Dupliquer un service existant", responses = {
            @ApiResponse(responseCode = "201", description = "Service dupliqué avec succès", content = @Content(schema = @Schema(implementation = ServiceDto.class)))
    })
    @PostMapping("/{id}/duplicate")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<ServiceDto> duplicate(@PathVariable Long id, @CurrentUser ApplicationUser user) {
        assertCanManageService(user, id);
        ServiceDto duplicated = serviceService.duplicateService(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(duplicated);
    }

    @Operation(summary = "Obtenir les statistiques détaillées d'un service")
    @GetMapping("/{id}/stats")
    public ResponseEntity<ServiceStatsDto> getServiceStats(@PathVariable Long id) {
        ServiceStatsDto stats = serviceService.getServiceStats(id);
        return ResponseEntity.ok(stats);
    }

}