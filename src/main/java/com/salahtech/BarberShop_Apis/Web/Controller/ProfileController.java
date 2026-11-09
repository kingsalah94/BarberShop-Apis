package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Dtos.UpdateProfileRequest;
import com.salahtech.BarberShop_Apis.Services.Implementations.FileStorageService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ApplicationUserService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/v1/users/profile") // Standard REST: 'users', pas 'user'
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "User Profile", description = "Gestion du profil utilisateur")
public class ProfileController {

    private final ApplicationUserService userService;
    private final FileStorageService fileStorageService;

    // --- ACTIONS SUR L'UTILISATEUR CONNECTÉ ---

    @Operation(summary = "Mon Profil")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationUserDto> getCurrentUser(@CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(ApplicationUserDto.fromEntity(user));
    }

    @Operation(summary = "Mettre à jour mon profil")
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()") // chacun peut éditer SON propre profil
    public ResponseEntity<ApplicationUserDto> updateMe(
            @CurrentUser ApplicationUser user,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateMe(user.getEmail(), req));
    }

    @Operation(summary = "Mettre à jour mon Avatar")
    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationUserDto> uploadAvatar(
            @CurrentUser ApplicationUser user,
            @RequestParam("file") MultipartFile file) {

        String fileUrl = fileStorageService.storeFile(file);
        // Le service doit gérer la sauvegarde en base
        return ResponseEntity.ok(userService.updateAvatar(user.getId(), fileUrl));
    }

    @Operation(summary = "Enregistrer Token FCM (Mobile)")
    @PostMapping("/me/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateFcmToken(
            @CurrentUser ApplicationUser user,
            @RequestBody Map<String, String> payload) {

        String token = payload.get("token");
        userService.saveFcmToken(user.getEmail(), token);
        return ResponseEntity.ok().build();
    }

    // --- ACTIONS ADMIN / READ ONLY ---

    @Operation(summary = "Voir un utilisateur par ID (Admin ou Soi-même)")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApplicationUserDto> getUserById(@PathVariable Long id) {
        ApplicationUser user = userService.findById(id);
        return ResponseEntity.ok(ApplicationUserDto.fromEntity(user));
    }
}