package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.SalonPostDto;
import com.salahtech.BarberShop_Apis.Services.Implementations.FileStorageService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonPostService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Publications (réalisations) d'un salon. Gestion = staff (OWNER + MANAGER) ;
 * la lecture publique passe par CatalogController.
 */
@RestController
@RequestMapping("/v1/salons/{salonId}/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Salon Posts", description = "Réalisations / portfolio d'un salon")
public class SalonPostController {

    private final SalonPostService postService;
    private final FileStorageService fileStorageService;
    private final TenantGuard tenantGuard;

    @Operation(summary = "Lister les publications du salon (staff)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalonPostDto>> list(@PathVariable Long salonId,
                                                   @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanFrontDesk(user, salonId);
        return ResponseEntity.ok(postService.listForSalon(salonId));
    }

    @Operation(summary = "Publier une réalisation (upload image + légende)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalonPostDto> create(@PathVariable Long salonId,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "caption", required = false) String caption,
                                               @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageStaff(user, salonId);
        final String imageUrl = fileStorageService.storeFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(salonId, imageUrl, caption));
    }

    @Operation(summary = "Supprimer une réalisation")
    @DeleteMapping("/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long salonId,
                                       @PathVariable Long postId,
                                       @CurrentUser ApplicationUser user) {
        tenantGuard.assertCanManageStaff(user, salonId);
        postService.delete(salonId, postId);
        return ResponseEntity.noContent().build();
    }
}
