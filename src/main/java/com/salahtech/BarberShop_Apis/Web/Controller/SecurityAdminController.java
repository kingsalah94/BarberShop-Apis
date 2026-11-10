package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Services.Interfaces.SecuritySettingsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import com.salahtech.BarberShop_Apis.repositories.RefreshTokenRepository;
import com.salahtech.BarberShop_Apis.AOP.LogActivity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/security")
@RequiredArgsConstructor
@Tag(name = "Security Admin", description = "Actions admin: révoquer tokens, rotation KID")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityAdminController {

    private final SecuritySettingsService settings;
    private final RefreshTokenRepository refreshTokens;

    /**
     * Révoque toutes les sessions : supprime les jetons de rafraîchissement stockés.
     * Chaque utilisateur (y compris l'admin) devra se reconnecter dès l'expiration de son jeton d'accès.
     */
    @PostMapping("/revoke-refresh-tokens")
    @Transactional
    @LogActivity(action = "REVOKE_ALL_SESSIONS", entityName = "RefreshToken")
    public ResponseEntity<Integer> revokeAllRefreshTokens() {
        int revoked = (int) refreshTokens.count();
        refreshTokens.deleteAllInBatch();
        return ResponseEntity.ok(revoked);
    }

    /** Fait tourner le KID JWT (maj settings + retourne les settings à jour) */
    @PostMapping("/rotate-kid")
    public ResponseEntity<?> rotateKid() {
        return ResponseEntity.ok(settings.rotateKid());
    }
}
