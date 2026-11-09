package com.salahtech.BarberShop_Apis.Web.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.salahtech.BarberShop_Apis.Dtos.CompleteProfileRequest;
import com.salahtech.BarberShop_Apis.Dtos.CreateUserWithProfileRequest.BarberProfileData;
import com.salahtech.BarberShop_Apis.Dtos.UserProfileDto;
import com.salahtech.BarberShop_Apis.Services.UserProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Gestion unifiée des profils utilisateur")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "Obtenir le profil complet de l'utilisateur connecté")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileDto> getMyProfile(Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.ok(userProfileService.getCompleteProfile(email));
    }

    @Operation(summary = "Compléter le profil après inscription")
    @PostMapping("/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileDto> completeProfile(
            Authentication auth,
            @Valid @RequestBody CompleteProfileRequest request) {
        String email = auth.getName();
        return ResponseEntity.ok(userProfileService.completeProfile(email, request));
    }

    @Operation(summary = "Changer le type d'utilisateur (CLIENT → BARBER)")
    @PostMapping("/upgrade-to-barber")
    // Auto-promotion CLIENT → BARBER désactivée (escalade de privilèges) :
    // les prestataires sont provisionnés par le salon. Réservé à l'ADMIN.
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> upgradeToBarber(
            Authentication auth,
            @Valid @RequestBody BarberProfileData barberData) {
        String email = auth.getName();
        return ResponseEntity.ok(userProfileService.upgradeToBarber(email, barberData));
    }
}
