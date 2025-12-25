package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BarberServices;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/barbers")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Barbers", description = "Gestion et recherche des barbiers")
public class BarberController {

    private final BarberServices barberService;
    // ❌ Suppression de BarberRepository : Le contrôleur ne doit pas toucher la BDD

    // ======= CRUD =======

    @Operation(summary = "Créer un profil barbier (Admin ou User)")
    @PostMapping
    @PreAuthorize("isAuthenticated()") // Sécurisé
    public ResponseEntity<BarberDto> create(@Valid @RequestBody BarberDto dto) {
        // La vérification "User a déjà un profil" DOIT être dans barberService.save()
        // Le contrôleur ne fait que passer le plat.
        BarberDto saved = barberService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Mettre à jour un barbier")
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER')")
    public ResponseEntity<BarberDto> update(@PathVariable Long id, @Valid @RequestBody BarberDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(barberService.save(dto));
    }

    @Operation(summary = "Trouver un barbier par ID")
    @GetMapping("/{id}")
    public ResponseEntity<BarberDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(barberService.findById(id));
    }

    @Operation(summary = "Supprimer un barbier")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        barberService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ======= RECHERCHES (Lectures Publiques) =======

    @GetMapping
    public ResponseEntity<List<BarberDto>> findAll() {
        return ResponseEntity.ok(barberService.findAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<BarberDto>> findAllAvailable() {
        return ResponseEntity.ok(barberService.findAllAvailable());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<BarberDto>> getNearbyBarbers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radius) {
        return ResponseEntity.ok(barberService.findNearby(lat, lng, radius));
    }

    @GetMapping("/search/location")
    public ResponseEntity<List<BarberDto>> findByLocationPaged(
            @RequestParam @NotBlank String q,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(barberService.findByLocation(q, pageable));
    }

    @GetMapping("/search/salon")
    public ResponseEntity<List<BarberDto>> findBySalonNamePaged(
            @RequestParam @NotBlank String q,
            @ParameterObject @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(barberService.findBySalonName(q, pageable));
    }

}