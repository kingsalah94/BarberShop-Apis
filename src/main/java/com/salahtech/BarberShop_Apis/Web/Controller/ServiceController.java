package com.salahtech.BarberShop_Apis.Web.Controller;


import com.salahtech.BarberShop_Apis.Dtos.ServiceDto;
import com.salahtech.BarberShop_Apis.Enums.ServiceCategory;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Validated
@Tag(name = "Services", description = "Gestion des prestations proposées par les barbiers/salons")
public class ServiceController {

    private final ServiceService serviceService;

    // ======= CRUD =======

    @Operation(summary = "Créer une prestation",
        responses = @ApiResponse(responseCode = "201",
            content = @Content(schema = @Schema(implementation = ServiceDto.class))))
    @PostMapping
    public ResponseEntity<ServiceDto> create(@Valid @RequestBody ServiceDto dto) {
        ServiceDto saved = serviceService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Mettre à jour une prestation par ID")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceDto> update(@PathVariable Long id, @Valid @RequestBody ServiceDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(serviceService.save(dto));
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ======= LECTURE LISTES =======

    @Operation(summary = "Lister toutes les prestations",
        responses = @ApiResponse(responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServiceDto.class)))))
    @GetMapping
    public ResponseEntity<List<ServiceDto>> findAll() {
        return ResponseEntity.ok(serviceService.findAll());
    }

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
}
