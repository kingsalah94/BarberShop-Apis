package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.*;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ApplicationUserService;
import com.salahtech.BarberShop_Apis.Utils.RateLimitUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Authentication", description = "Inscription, Connexion, Sécurité")
public class AuthController {

    private final ApplicationUserService userService;
    private final RateLimitUtil rateLimitUtil;

    @Operation(summary = "Connexion (Login)")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request,
                                                 HttpServletRequest http) {
        // Limitation par IP + email (et non globale : 5 échecs ne doivent pas bloquer toute l'application)
        String key = clientIp(http) + "|" + String.valueOf(request.getEmail()).trim().toLowerCase();
        if (rateLimitUtil != null && !rateLimitUtil.tryConsume(key, "login")) {
            return ResponseEntity.status(429).build();
        }
        return ResponseEntity.ok(userService.login(request));
    }

    @Operation(summary = "Inscription (Register) — public : crée uniquement des CLIENTS")
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        // Inscription publique = CLIENT uniquement. Les comptes pro (owner/manager/
        // réception/prestataire/admin) sont créés via d'autres flux (admin, sous-comptes,
        // « Mon équipe »). On force donc le type quel que soit le payload.
        request.setUserType(com.salahtech.BarberShop_Apis.Enums.ApplicationUserType.CLIENT);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @Operation(summary = "Rafraîchir le token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(userService.refreshToken(request.getRefreshToken()));
    }

    @Operation(summary = "Déconnexion")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        userService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vérifier l'email")
    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok("Email vérifié avec succès.");
    }

    // --- GESTION MOT DE PASSE ---

    @Operation(summary = "Mot de passe oublié (Demande Email)")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest request, HttpServletRequest http) {
        if (rateLimitUtil != null && !rateLimitUtil.tryConsume(clientIp(http), "forgot-password")) {
            return ResponseEntity.status(429).build();
        }
        userService.forgotPassword(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Réinitialiser le mot de passe (via Token)")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Changer le mot de passe (Utilisateur connecté)")
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               Authentication auth, HttpServletRequest http) {
        // /v1/auth/** est public : on exige ici un utilisateur authentifié et on IGNORE
        // l'email du corps (sinon force brute de l'ancien mot de passe de n'importe qui).
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (rateLimitUtil != null && !rateLimitUtil.tryConsume(auth.getName(), "change-password")) {
            return ResponseEntity.status(429).build();
        }
        request.setEmail(auth.getName());
        // La logique (Check old password + Confirm new) doit être dans le Service !
        userService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    /** IP client (derrière un reverse proxy, configurer server.forward-headers-strategy). */
    private static String clientIp(HttpServletRequest http) {
        return http.getRemoteAddr();
    }
}