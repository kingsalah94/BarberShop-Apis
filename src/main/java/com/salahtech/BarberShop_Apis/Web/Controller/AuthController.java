package com.salahtech.BarberShop_Apis.Web.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Dtos.AuthRequestDTO;
import com.salahtech.BarberShop_Apis.Dtos.AuthResponseDTO;
import com.salahtech.BarberShop_Apis.Dtos.RegisterRequestDTO;
import com.salahtech.BarberShop_Apis.Dtos.UpdateProfileRequest;
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
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Inscription, connexion, refresh token, mot de passe et vérification email")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    private final ApplicationUserService userService;

    @Autowired
    private RateLimitUtil rateLimitUtil;

    // ======================
    // AUTH
    // ======================

    // ======================
    // AUTH ENDPOINTS Register
    // ======================
    @Operation(summary = "Inscription (register)", responses = {
            @ApiResponse(responseCode = "201", description = "Compte créé", content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        var response = userService.register(request); // envoie email de vérification côté service
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ======================
    // AUTH ENDPOINTS Profile
    // ======================
    @Operation(summary = "Mettre à jour le profil courant")
    @PutMapping("/me")
    public ResponseEntity<ApplicationUserDto> updateMe(@Valid @RequestBody UpdateProfileRequest req,
            Authentication authentication) {
        var dto = userService.updateMe(authentication.getName(), req);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Récupérer l'utilisateur courant", security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "Utilisateur courant", content = @Content(schema = @Schema(implementation = ApplicationUserDto.class)))
    })
    @GetMapping("/currentUser")
    public ResponseEntity<ApplicationUserDto> currentUser(Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(ApplicationUserDto.fromEntity(user));
    }

    // ======================
    // AUTH ENDPOINTS Login/Refresh/Logout
    // ======================
    @Operation(summary = "Connexion (login)", responses = {
            @ApiResponse(responseCode = "200", description = "Connecté", content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Identifiants invalides / Compte désactivé ou non vérifié"),
            @ApiResponse(responseCode = "429", description = "Trop de tentatives (rate-limit)")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        // Rate-limit très simple (si util présent)
        if (rateLimitUtil != null && !rateLimitUtil.tryConsume("global", "login")) {
            return ResponseEntity.status(429)
                    .body(null);
        }
        return ResponseEntity.ok(userService.login(request));
    }

    // =======================
    // AUTH ENDPOINTS Refresh/Logout
    // ======================
    @Operation(summary = "Renouveler le token d'accès (refresh token)", responses = {
            @ApiResponse(responseCode = "200", description = "Nouveaux tokens", content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Refresh token invalide ou expiré")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(userService.refreshToken(request.getRefreshToken()));
    }

    // =======================
    // AUTH ENDPOINTS Logout
    // ======================
    @Operation(summary = "Déconnexion (révoque le refresh token)", responses = {
            @ApiResponse(responseCode = "204", description = "Déconnecté"),
            @ApiResponse(responseCode = "400", description = "Requête invalide")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        userService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    // ======================
    // EMAIL & PASSWORD
    // ======================

    @Operation(summary = "Demander un email de réinitialisation de mot de passe", responses = {
            @ApiResponse(responseCode = "204", description = "Email de réinitialisation envoyé (si compte existant)"),
            @ApiResponse(responseCode = "404", description = "Email inconnu")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
        userService.forgotPassword(request.getEmail()); // envoie l'email côté service
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Réinitialiser le mot de passe (avec token reçu par email)", responses = {
            @ApiResponse(responseCode = "204", description = "Mot de passe réinitialisé"),
            @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getToken(), request.getNewPassword()); // envoie confirmation côté service
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vérifier l'email", responses = {
            @ApiResponse(responseCode = "200", description = "Email vérifié"),
            @ApiResponse(responseCode = "400", description = "Token invalide")
    })
    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam("token") String token) {
        userService.verifyEmail(token); // envoie email de bienvenue côté service
        return ResponseEntity.ok(new MessageResponse("Email vérifié avec succès."));
    }

    // ======================
    // PROFIL
    // ======================

    @Operation(summary = "Récupérer l'utilisateur courant", security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "Utilisateur courant", content = @Content(schema = @Schema(implementation = ApplicationUserDto.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<ApplicationUserDto> me(Authentication authentication) {
        // authentication.getName() porte l'email/username
        var user = userService.findByEmail(authentication.getName());
        // convertToUserDTO est interne au service, donc on expose l’entité via le DTO
        // du service register/login:
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
    // REQUEST/RESPONSE DTOs LOCAUX
    // (tu peux les déplacer dans ton package Dtos si tu préfères)
    // ======================

    @Schema(name = "RefreshTokenRequest")
    public static class RefreshTokenRequest {
        @NotBlank
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    @Schema(name = "EmailRequest")
    public static class EmailRequest {
        @NotBlank
        @Email
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    @Schema(name = "ResetPasswordRequest")
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank
        private String newPassword;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    @Schema(name = "MessageResponse")
    public static class MessageResponse {
        private String message;

        public MessageResponse() {
        }

        public MessageResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

}