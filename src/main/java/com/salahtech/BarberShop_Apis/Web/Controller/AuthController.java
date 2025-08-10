package com.salahtech.BarberShop_Apis.Web.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Dtos.AuthRequestDTO;
import com.salahtech.BarberShop_Apis.Dtos.AuthResponseDTO;
import com.salahtech.BarberShop_Apis.Dtos.RegisterRequestDTO;
import com.salahtech.BarberShop_Apis.Services.Implementations.AuthService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ApplicationUserService;
import com.salahtech.BarberShop_Apis.Utils.RateLimitUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur d'authentification et gestion du compte.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Inscription, connexion, refresh token, mot de passe et vérification email")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private final ApplicationUserService userService;

    @Autowired
    private RateLimitUtil rateLimitUtil;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDTO authRequest,
                                  HttpServletRequest request) {
        
        // Vérifier le rate limiting
        String clientIp = getClientIpAddress(request);
        if (!rateLimitUtil.tryConsume(clientIp, "login")) {
            return ResponseEntity.status(429)
                    .body(Map.of("message", "Trop de tentatives de connexion. Réessayez dans 5 minutes."));
        }
        
        try {
            AuthResponseDTO response = authService.login(authRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
    
    @PostMapping("/registers")
    public ResponseEntity<?> registers(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        try {
            AuthResponseDTO response = authService.register(registerRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
    
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Refresh token manquant"));
            }
            
            AuthResponseDTO response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            authService.logout(refreshToken);
            return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Email manquant"));
            }
            
            authService.forgotPassword(email);
            return ResponseEntity.ok(Map.of("message", "Email de réinitialisation envoyé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            
            if (token == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Token ou mot de passe manquant"));
            }
            
            authService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
    
    @GetMapping("/verify-emails")
    public ResponseEntity<?> verifyEmails(@RequestParam String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(Map.of("message", "Email vérifié avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0];
        }
    }

    // ======================
    //  AUTH
    // ======================

    @Operation(summary = "Inscription (register)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Compte créé",
                            content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Requête invalide")
            })
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        AuthResponseDTO response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Connexion (login)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Connecté",
                            content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Identifiants invalides / Compte désactivé ou non vérifié")
            })
    @PostMapping("/user/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @Operation(summary = "Renouveler le token d'accès (refresh token)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Nouveaux tokens",
                            content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Refresh token invalide ou expiré")
            })
    @PostMapping("/user/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(userService.refreshToken(request.getRefreshToken()));
    }

    @Operation(summary = "Déconnexion (révoque le refresh token)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Déconnecté"),
                    @ApiResponse(responseCode = "400", description = "Requête invalide")
            })
    @PostMapping("/user/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        userService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    // ======================
    //  EMAIL & PASSWORD
    // ======================

    @Operation(summary = "Demander un email de réinitialisation de mot de passe",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Email de réinitialisation envoyé (si compte existant)"),
                    @ApiResponse(responseCode = "404", description = "Email inconnu")
            })
    @PostMapping("/user/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
        userService.forgotPassword(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Réinitialiser le mot de passe (avec token reçu par email)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Mot de passe réinitialisé"),
                    @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
            })
    @PostMapping("/user/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vérifier l'email",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Email vérifié"),
                    @ApiResponse(responseCode = "400", description = "Token invalide")
            })
    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam("token") String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponse("Email vérifié avec succès."));
    }

    // ======================
    //  PROFIL
    // ======================

    @Operation(summary = "Récupérer l'utilisateur courant",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Utilisateur courant",
                            content = @Content(schema = @Schema(implementation = ApplicationUserDto.class)))
            })
    @GetMapping("/me")
    public ResponseEntity<ApplicationUserDto> me(Authentication authentication) {
        // authentication.getName() porte l'email/username
        var user = userService.findByEmail(authentication.getName());
        // convertToUserDTO est interne au service, donc on expose l’entité via le DTO du service register/login:
        ApplicationUserDto dto = ApplicationUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .isEnabled(user.isEnabled())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .roles(user.getRoles() != null
                        ? user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet())
                        : java.util.Collections.emptySet())
                .permissions(user.getRoles() != null
                        ? user.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .map(p -> p.getName())
                        .collect(java.util.stream.Collectors.toSet())
                        : java.util.Collections.emptySet())
                .build();

        return ResponseEntity.ok(dto);
    }

    // ======================
    //  REQUEST/RESPONSE DTOs LOCAUX
    //  (tu peux les déplacer dans ton package Dtos si tu préfères)
    // ======================

    @Schema(name = "RefreshTokenRequest")
    public static class RefreshTokenRequest {
        @NotBlank
        private String refreshToken;
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    @Schema(name = "EmailRequest")
    public static class EmailRequest {
        @NotBlank @Email
        private String email;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @Schema(name = "ResetPasswordRequest")
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank
        private String newPassword;
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    @Schema(name = "MessageResponse")
    public static class MessageResponse {
        private String message;
        public MessageResponse() {}
        public MessageResponse(String message) { this.message = message; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}