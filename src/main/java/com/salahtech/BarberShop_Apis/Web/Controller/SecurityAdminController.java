package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Services.Interfaces.SecuritySettingsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/security")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Security Admin", description = "Actions admin: révoquer tokens, rotation KID")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityAdminController {

    private final SecuritySettingsService settings;

    /** Révoque tous les refresh tokens (stub: renvoie 0 si pas encore de store) */
    @PostMapping("/revoke-refresh-tokens")
    public ResponseEntity<Integer> revokeAllRefreshTokens() {
        // TODO: brancher sur ton store de refresh tokens si tu en as un
        int revoked = 0;
        return ResponseEntity.ok(revoked);
    }

    /** Fait tourner le KID JWT (maj settings + retourne les settings à jour) */
    @PostMapping("/rotate-kid")
    public ResponseEntity<?> rotateKid() {
        return ResponseEntity.ok(settings.rotateKid());
    }
}
