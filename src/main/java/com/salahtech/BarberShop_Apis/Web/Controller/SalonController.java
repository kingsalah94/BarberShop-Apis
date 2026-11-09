package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Dtos.CreateMemberRequest;
import com.salahtech.BarberShop_Apis.Dtos.CreateSalonRequest;
import com.salahtech.BarberShop_Apis.Dtos.MemberDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonDto;
import com.salahtech.BarberShop_Apis.Dtos.UpdateSalonRequest;
import com.salahtech.BarberShop_Apis.Services.Implementations.FileStorageService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonMembershipService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Gestion des salons (tenant). Chaque endpoint applique l'isolation
 * via {@link TenantGuard} : un SALON_OWNER ne voit/gère que ses salons,
 * l'ADMIN a un accès total.
 */
@RestController
@RequestMapping("/v1/salons")
@RequiredArgsConstructor
@Tag(name = "Salons", description = "Gestion des salons (multi-tenant)")
public class SalonController {

    private final SalonService salonService;
    private final SalonMembershipService membershipService;
    private final TenantGuard tenantGuard;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Créer un salon (le propriétaire est l'utilisateur courant)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALON_OWNER')")
    public ResponseEntity<SalonDto> create(@Valid @RequestBody CreateSalonRequest req,
                                           @CurrentUser ApplicationUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salonService.create(req, user));
    }

    @Operation(summary = "Mes salons (selon mes accès : owner / manager / réceptionniste)")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalonDto>> mySalons(@CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(salonService.findMine(user));
    }

    @Operation(summary = "Lister tous les salons (ADMIN)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SalonDto>> findAll() {
        return ResponseEntity.ok(salonService.findAll());
    }

    @Operation(summary = "Détails d'un salon")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonDto> getById(@PathVariable Long id,
                                            @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, id); // tout membre du salon peut consulter
        return ResponseEntity.ok(salonService.findById(id));
    }

    @Operation(summary = "Mettre à jour un salon")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonDto> update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateSalonRequest req,
                                           @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageSettings(user, id); // OWNER + MANAGER
        return ResponseEntity.ok(salonService.update(id, req));
    }

    @Operation(summary = "Uploader le logo du salon")
    @PostMapping("/{id}/logo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonDto> uploadLogo(@PathVariable Long id,
                                               @RequestParam("file") MultipartFile file,
                                               @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageSettings(user, id);
        String url = fileStorageService.storeFile(file);
        return ResponseEntity.ok(salonService.setLogo(id, url));
    }

    @Operation(summary = "Uploader la photo de couverture du salon")
    @PostMapping("/{id}/cover")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonDto> uploadCover(@PathVariable Long id,
                                                @RequestParam("file") MultipartFile file,
                                                @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageSettings(user, id);
        String url = fileStorageService.storeFile(file);
        return ResponseEntity.ok(salonService.setCover(id, url));
    }

    @Operation(summary = "Supprimer un salon")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @CurrentUser ApplicationUser user) {
        tenantGuard.assertOwner(user, id); // OWNER uniquement
        salonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Barbiers rattachés à un salon")
    @GetMapping("/{id}/barbers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BarberDto>> barbers(@PathVariable Long id,
                                                   @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, id); // visible par tout membre (file, affectations)
        return ResponseEntity.ok(salonService.findBarbers(id));
    }

    @Operation(summary = "Rattacher un barbier au salon")
    @PostMapping("/{id}/barbers/{barberId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> attachBarber(@PathVariable Long id,
                                             @PathVariable Long barberId,
                                             @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageStaff(user, id); // OWNER + MANAGER
        salonService.attachBarber(id, barberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Détacher un barbier du salon")
    @DeleteMapping("/{id}/barbers/{barberId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> detachBarber(@PathVariable Long id,
                                             @PathVariable Long barberId,
                                             @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageStaff(user, id); // OWNER + MANAGER
        salonService.detachBarber(id, barberId);
        return ResponseEntity.noContent().build();
    }

    // ===== Sous-comptes du salon (Owner / Manager / Réceptionniste) =====

    @Operation(summary = "Lister les sous-comptes d'un salon")
    @GetMapping("/{id}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MemberDto>> members(@PathVariable Long id,
                                                   @CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(membershipService.listMembers(user, id));
    }

    @Operation(summary = "Créer un sous-compte (manager / réceptionniste)")
    @PostMapping("/{id}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemberDto> addMember(@PathVariable Long id,
                                               @Valid @RequestBody CreateMemberRequest req,
                                               @CurrentUser ApplicationUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(membershipService.createMember(user, id, req));
    }

    @Operation(summary = "Retirer un sous-compte du salon")
    @DeleteMapping("/{id}/members/{membershipId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeMember(@PathVariable Long id,
                                             @PathVariable Long membershipId,
                                             @CurrentUser ApplicationUser user) {
        membershipService.revokeMember(user, id, membershipId);
        return ResponseEntity.noContent().build();
    }
}
