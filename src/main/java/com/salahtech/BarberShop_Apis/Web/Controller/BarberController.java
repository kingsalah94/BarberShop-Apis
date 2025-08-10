package com.salahtech.BarberShop_Apis.Web.Controller;


import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BarberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/barbers")
@RequiredArgsConstructor
@Validated
@Tag(name = "Barbers", description = "Gestion et recherche des barbiers")
public class BarberController {

    private final BarberService barberService;

    // ======= CRUD =======

    @Operation(summary = "Créer un barbier",
        responses = {
            @ApiResponse(responseCode = "201", description = "Créé",
                content = @Content(schema = @Schema(implementation = BarberDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation invalide")
        })
    @PostMapping
    public ResponseEntity<BarberDto> create(@Valid @RequestBody BarberDto dto) {
        BarberDto saved = barberService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Mettre à jour un barbier")
    @PutMapping("/{id}")
    public ResponseEntity<BarberDto> update(@PathVariable Long id, @Valid @RequestBody BarberDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(barberService.save(dto));
    }

    @Operation(summary = "Trouver un barbier par ID")
    @GetMapping("/{id}")
    public ResponseEntity<BarberDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(barberService.findById(id));
    }

    @Operation(summary = "Supprimer un barbier par ID", responses = {
        @ApiResponse(responseCode = "204", description = "Supprimé")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        barberService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ======= LECTURE LISTES (non paginées) =======

    @Operation(summary = "Lister tous les barbiers",
        responses = @ApiResponse(responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BarberDto.class)))))
    @GetMapping
    public ResponseEntity<List<BarberDto>> findAll() {
        return ResponseEntity.ok(barberService.findAll());
    }

    @Operation(summary = "Lister tous les barbiers disponibles")
    @GetMapping("/available")
    public ResponseEntity<List<BarberDto>> findAllAvailable() {
        return ResponseEntity.ok(barberService.findAllAvailable());
    }

    @Operation(summary = "Trouver un barbier par l'ID utilisateur")
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<BarberDto> findByUserId(@PathVariable @NotNull Long userId) {
        return ResponseEntity.ok(barberService.findByUserId(userId));
    }

    // ======= RECHERCHES PAGINÉES/TRIABLES =======

    @Operation(
        summary = "Recherche paginée: par localisation (contient)",
        description = "Exemple: /search/location?q=Niamey&page=0&size=10&sort=rating,desc",
        parameters = {
            @Parameter(name = "q", description = "Fragment de localisation (contains)", example = "Niamey")
        })
    @GetMapping("/search/location")
    public ResponseEntity<List<BarberDto>> findByLocationPaged(
            @RequestParam @NotBlank String q,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(barberService.findByLocation(q, pageable));
    }

    @Operation(
        summary = "Recherche paginée: par nom de salon (contient)",
        description = "Exemple: /search/salon?q=Barber&page=0&size=12&sort=salonName,asc")
    @GetMapping("/search/salon")
    public ResponseEntity<List<BarberDto>> findBySalonNamePaged(
            @RequestParam @NotBlank String q,
            @ParameterObject @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(barberService.findBySalonName(q, pageable));
    }

    @Operation(
        summary = "Recherche paginée: par fourchette de prix",
        description = "Exemple: /search/price-range?min=2000&max=8000&sort=priceFrom,asc")
    @GetMapping("/search/price-range")
    public ResponseEntity<List<BarberDto>> findByPriceRangePaged(
            @RequestParam @NotNull @DecimalMin("0.0") BigDecimal min,
            @RequestParam @NotNull @DecimalMin("0.0") BigDecimal max,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(barberService.findByPriceRange(min, max, pageable));
    }

    @Operation(
        summary = "Recherche paginée: par note minimale",
        description = "Exemple: /search/min-rating?minRating=4.3&sort=rating,desc")
    @GetMapping("/search/min-rating")
    public ResponseEntity<List<BarberDto>> findByMinRatingPaged(
            @RequestParam @NotNull @DecimalMin("0.0") BigDecimal minRating,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(barberService.findByMinRating(minRating, pageable));
    }

    @Operation(
        summary = "Recherche paginée: barbiers disponibles par localisation",
        description = "Exemple: /search/available-by-location?q=Plateau&sort=rating,desc")
    @GetMapping("/search/available-by-location")
    public ResponseEntity<List<BarberDto>> findAvailableByLocationPaged(
            @RequestParam @NotBlank String q,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(barberService.findAvailableByLocation(q, pageable));
    }
}
