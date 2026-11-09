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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Inscription, Connexion, Sécurité")
public class AuthController {

    private final ApplicationUserService userService;
    private final RateLimitUtil rateLimitUtil;

    /**
     * Mode « cookie » (client web, en-tête {@code X-Auth-Mode: cookie}) : le refresh token
     * est posé dans un cookie HttpOnly et n'est JAMAIS renvoyé dans le JSON — un script
     * injecté (XSS) ne peut donc pas le voler. Sans l'en-tête (mobile), comportement
     * inchangé : tokens dans le corps de la réponse.
     */
    static final String AUTH_MODE_HEADER = "X-Auth-Mode";
    static final String REFRESH_COOKIE = "bb_refresh";

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.refresh-token-expiration:2592000000}")
    private long refreshTtlMs;

    @Operation(summary = "Connexion (Login)")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request,
                                                 HttpServletRequest http) {
        // Limitation par IP + email (et non globale : 5 échecs ne doivent pas bloquer toute l'application)
        String key = clientIp(http) + "|" + String.valueOf(request.getEmail()).trim().toLowerCase();
        if (rateLimitUtil != null && !rateLimitUtil.tryConsume(key, "login")) {
            return ResponseEntity.status(429).build();
        }
        return withRefreshCookie(http, userService.login(request));
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

    @Operation(summary = "Rafraîchir le token (corps JSON ou cookie HttpOnly du client web)")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                                   @CookieValue(name = REFRESH_COOKIE, required = false) String cookie,
                                                   HttpServletRequest http) {
        String token = refreshTokenFrom(request, cookie);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return withRefreshCookie(http, userService.refreshToken(token));
    }

    @Operation(summary = "Déconnexion")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                       @CookieValue(name = REFRESH_COOKIE, required = false) String cookie) {
        String token = refreshTokenFrom(request, cookie);
        if (token != null) {
            userService.logout(token);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
                .build();
    }

    private static String refreshTokenFrom(RefreshTokenRequest body, String cookie) {
        if (body != null && body.getRefreshToken() != null && !body.getRefreshToken().isBlank()) {
            return body.getRefreshToken();
        }
        return cookie != null && !cookie.isBlank() ? cookie : null;
    }

    /** Client web : refresh token → cookie HttpOnly et retiré du JSON. Mobile : inchangé. */
    private ResponseEntity<AuthResponseDTO> withRefreshCookie(HttpServletRequest http, AuthResponseDTO body) {
        if (!"cookie".equalsIgnoreCase(http.getHeader(AUTH_MODE_HEADER)) || body == null
                || body.getRefreshToken() == null) {
            return ResponseEntity.ok(body);
        }
        ResponseCookie cookie = refreshCookie(body.getRefreshToken(), refreshTtlMs / 1000);
        body.setRefreshToken(null);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(body);
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAgeSeconds)
                .build();
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