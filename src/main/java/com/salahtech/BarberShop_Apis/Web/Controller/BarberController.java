package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Services.Implementations.FileStorageService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BarberServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // IMPORTANT
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // IMPORTANT

import java.util.List;

@RestController
@RequestMapping("/v1/barbers")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Barbers", description = "Gestion et recherche des barbiers")
public class BarberController {

    private final BarberServices barberService;
    private final FileStorageService fileStorageService;
    private final com.salahtech.BarberShop_Apis.repositories.BarberRepository barberRepository;
    private final com.salahtech.BarberShop_Apis.Config.TenantGuard tenantGuard;

    // ======= CRUD =======

    @Operation(summary = "Créer un profil barbier (Admin ou User) avec Image")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // ✅ Consumes Multipart
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BarberDto> create(
            @RequestPart("barber") @Valid BarberDto dto, // ✅ RequestPart pour le JSON
            @RequestPart(value = "file", required = false) MultipartFile file, // ✅ RequestPart pour le Fichier
            @com.salahtech.BarberShop_Apis.Config.CurrentUser com.salahtech.BarberShop_Apis.models.ApplicationUser user
    ) {
        // Un non-admin ne peut créer un profil barbier QUE pour lui-même.
        if (!tenantGuard.isAdmin(user)) {
            dto.setUserId(user.getId());
            dto.setUserDto(null);
        }
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = fileStorageService.storeFile(file);
        }
        // On appelle la méthode create du service (pas save générique)
        BarberDto saved = barberService.create(dto, imageUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Mettre à jour un barbier avec Image")
    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // ✅ Consumes Multipart
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBER')")
    public ResponseEntity<BarberDto> update(
            @PathVariable Long id,
            @RequestPart("barber") @Valid BarberDto dto, // ✅ RequestPart JSON
            @RequestPart(value = "file", required = false) MultipartFile file, // ✅ RequestPart Fichier
            @com.salahtech.BarberShop_Apis.Config.CurrentUser com.salahtech.BarberShop_Apis.models.ApplicationUser user
    ) {
        if (!tenantGuard.isAdmin(user)) {
            Long mine = barberRepository.findByUser_Id(user.getId()).map(b -> b.getId()).orElse(null);
            Long salonId = barberRepository.findSalonIdById(id).orElse(null);
            boolean ok = id.equals(mine) || (salonId != null && tenantGuard.isSalonStaff(user, salonId));
            if (!ok) throw new org.springframework.security.access.AccessDeniedException("Accès refusé : profil d'un autre barbier");
        }
        String newImageUrl = null;
        dto.setId(id);
        if (file != null && !file.isEmpty()) {
            newImageUrl = fileStorageService.storeFile(file);
        }
        // On appelle la méthode update du service
        return ResponseEntity.ok(barberService.update(id, dto, newImageUrl));
    }

    // ... Le reste (GET, DELETE, SEARCH) ne change pas ...

    @Operation(summary = "Trouver un barbier par ID")
    @GetMapping("/{id}")
    public ResponseEntity<BarberDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(barberService.findById(id));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<BarberDto> getBarberByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(barberService.findBarberByUserId(userId));
    }

    @Operation(summary = "Supprimer un barbier")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        barberService.delete(id);
        return ResponseEntity.noContent().build();
    }

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