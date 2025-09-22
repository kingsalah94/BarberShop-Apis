package com.salahtech.BarberShop_Apis.Services.Implementations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Dtos.AuthRequestDTO;
import com.salahtech.BarberShop_Apis.Dtos.AuthResponseDTO;
import com.salahtech.BarberShop_Apis.Dtos.RegisterRequestDTO;
import com.salahtech.BarberShop_Apis.Dtos.UpdateProfileRequest;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.EmailService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ApplicationUserService;
import com.salahtech.BarberShop_Apis.Utils.JwtUtil;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.models.RefreshToken;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.ClientRepository;
import com.salahtech.BarberShop_Apis.reppsitories.RefreshTokenRepository;
import com.salahtech.BarberShop_Apis.reppsitories.RoleRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import com.salahtech.BarberShop_Apis.Exceptions.BadRequestException;
import com.salahtech.BarberShop_Apis.models.Permission;
import java.util.UUID;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ApplicationUserServiceImpl implements ApplicationUserService {

    @Autowired
    private ApplicationUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private EmailService emailService;

    @Override
    public ApplicationUser findByEmailOrCreate(String email, String name, AuthProvider provider, String providerId) {
        var existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            var u = existing.get();
            u.setLastLogin(LocalDateTime.now());
            return userRepository.save(u);
        }

        ApplicationUser u = new ApplicationUser();
        u.setEmail(email);
        var parts = name != null ? name.split(" ", 2) : new String[] { "", "" };
        u.setFirstName(parts[0]);
        u.setLastName(parts.length > 1 ? parts[1] : "");
        u.setProvider(provider);
        u.setProviderId(providerId);
        u.setIsEnabled(true);
        u.setIsVerified(true);
        u.setPassword("");
        u.setUserType(ApplicationUserType.CLIENT);

        Role clientRole = roleRepository.findByName("CLIENT")
                .orElseThrow(() -> new ResourceNotFoundException("Rôle CLIENT non trouvé"));
        u.setRoles(Set.of(clientRole));

        u = userRepository.save(u);

        // ✅ Envoi de bienvenue pour les comptes créés via OAuth2
        try {
            emailService.sendWelcomeEmail(u);
        } catch (Exception ignored) {
        }

        return u;
    }

    @Override
    public ApplicationUser findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID : " + id));
    }

    @Override
    public ApplicationUser findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'email : " + email));
    }

    @Override
    public AuthResponseDTO login(AuthRequestDTO authRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getEmail(), authRequest.getPassword()));

        UserDetails principal = (UserDetails) auth.getPrincipal();
        ApplicationUser user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!user.isEnabled())
            throw new BadRequestException("Compte désactivé. Contactez l'administrateur.");
        if (!Boolean.TRUE.equals(user.getIsVerified()))
            throw new BadRequestException("Email non vérifié. Vérifiez votre boîte mail.");

        String accessToken = jwtUtil.generateAccessToken(principal);
        String refreshToken = jwtUtil.generateRefreshToken(principal);

        saveRefreshToken(user, refreshToken);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // (Facultatif) — notifier la connexion réussie
        // try { emailService.sendTemplateEmail(user.getEmail(), "Connexion réussie",
        // "login-success", Map.of("firstName", user.getFirstName())); } catch
        // (Exception ignored) {}

        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                convertToUserDTO(user));
    }

    @Override
    public AuthResponseDTO register(RegisterRequestDTO req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new BadRequestException("Un compte avec cet email existe déjà");

        ApplicationUser u = new ApplicationUser();
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setPhone(req.getPhone());
        u.setCreatedAt(LocalDateTime.now());
        u.setIsEnabled(true);
        u.setIsVerified(false);
        u.setVerificationToken(UUID.randomUUID().toString());
        u.setProvider(AuthProvider.LOCAL);
        ApplicationUserType type = req.getUserType() != null ? req.getUserType() : ApplicationUserType.CLIENT;
        u.setUserType(type);
        u.setRoles(Set.of(getRoleByUserType(type)));

        u = userRepository.save(u);

        // 2. Créer automatiquement le profil métier selon le type
        createBusinessProfile(u);
        // ✅ email de vérification à l’inscription
        try {
            emailService.sendVerificationEmail(u);
        } catch (Exception ignored) {
        }

        String accessToken = jwtUtil.generateAccessToken(u);
        String refreshToken = jwtUtil.generateRefreshToken(u);
        saveRefreshToken(u, refreshToken);

        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                convertToUserDTO(u));
    }

    /**
     * Crée automatiquement le profil métier (Client ou Barber) selon le type
     * d'utilisateur
     */
    private void createBusinessProfile(ApplicationUser user) {
        switch (user.getUserType()) {
            case CLIENT -> createClientProfile(user);
            case BARBER -> createBarberProfile(user);
            case SALON_OWNER -> createBarberProfile(user); // Un salon owner est aussi un barber
            case ADMIN -> {
                /* Pas de profil métier pour admin */ }
        }
    }

    private void createClientProfile(ApplicationUser user) {
        if (!clientRepository.existsByUser_Id(user.getId())) {
            Client client = Client.builder()
                    .user(user)
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .build();

            client = clientRepository.save(client);
            user.setClient(client); // Mise à jour bidirectionnelle
            log.info("Client profile created for user {}", user.getId());
        }
    }

    private void createBarberProfile(ApplicationUser user) {
        if (!barberRepository.existsByUser_Id(user.getId())) {
            Barber barber = Barber.builder()
                    .user(user)
                    .name(user.getFullName())
                    .salonName("À définir") // Valeur par défaut
                    .location("À définir")
                    .available(false) // Non disponible par défaut
                    .priceFrom(BigDecimal.ZERO)
                    .build();

            barber = barberRepository.save(barber);
            user.setBarber(barber); // Mise à jour bidirectionnelle
            log.info("Barber profile created for user {}", user.getId());
        }
    }

    @Override
    public AuthResponseDTO refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken))
            throw new BadRequestException("Refresh token invalide ou expiré");

        String username = jwtUtil.extractUsername(refreshToken);
        ApplicationUser u = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Refresh token non trouvé"));
        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new BadRequestException("Refresh token expiré");
        }

        String newAccess = jwtUtil.generateAccessToken(u);
        String newRefresh = jwtUtil.generateRefreshToken(u);

        refreshTokenRepository.delete(stored);
        saveRefreshToken(u, newRefresh);

        // (Facultatif) — notifier la rotation de token
        // try { emailService.sendTemplateEmail(u.getEmail(), "Sécurité du compte",
        // "token-rotated", Map.of()); } catch (Exception ignored) {}

        return new AuthResponseDTO(
                newAccess,
                newRefresh,
                jwtUtil.getAccessTokenExpiration(),
                convertToUserDTO(u));
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(refreshTokenRepository::delete);
        }
    }

    @Override
    public void forgotPassword(String email) {
        ApplicationUser u = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte trouvé avec cet email"));

        String resetToken = UUID.randomUUID().toString();
        u.setResetPasswordToken(resetToken);
        u.setResetPasswordExpires(LocalDateTime.now().plusHours(1));
        userRepository.save(u);

        // ✅ email reset password (lien/code)
        try {
            emailService.sendPasswordResetEmail(u, resetToken);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        ApplicationUser u = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new BadRequestException("Token de reset invalide"));

        if (u.getResetPasswordExpires() == null || u.getResetPasswordExpires().isBefore(LocalDateTime.now()))
            throw new BadRequestException("Token de reset expiré");

        u.setPassword(passwordEncoder.encode(newPassword));
        u.setResetPasswordToken(null);
        u.setResetPasswordExpires(null);
        userRepository.save(u);

        // Invalider les refresh tokens existants
        refreshTokenRepository.deleteByUser(u);

        // ✅ email de confirmation de changement de mot de passe
        try {
            emailService.sendPasswordChangedConfirmation(u);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void verifyEmail(String token) {
        ApplicationUser u = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Token de vérification invalide"));

        u.setIsVerified(true);
        u.setVerificationToken(null);
        userRepository.save(u);

        // ✅ email de bienvenue après activation
        try {
            emailService.sendWelcomeEmail(u);
        } catch (Exception ignored) {
        }
    }

    // ApplicationUserServiceImpl
    @Override
    @Transactional
    public ApplicationUserDto updateMe(String email, UpdateProfileRequest req) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (req.getFirstName() != null)
            user.setFirstName(req.getFirstName());
        if (req.getLastName() != null)
            user.setLastName(req.getLastName());
        if (req.getPhone() != null)
            user.setPhone(req.getPhone());
        if (req.getProfileImageUrl() != null)
            user.setProfileImageUrl(req.getProfileImageUrl());
        if (req.getPreferences() != null)
            user.setPreferences(req.getPreferences());
        // updatedAt via @PreUpdate
        user = userRepository.save(user);
        return ApplicationUserDto.fromEntity(user);
    }

    @Override
    public void saveRefreshToken(ApplicationUser user, String tokenValue) {
        // Supprimer l'ancien refresh token s'il existe
        refreshTokenRepository.findByUser(user)
                .ifPresent(refreshTokenRepository::delete);

        // Créer et sauvegarder le nouveau refresh token
        RefreshToken refreshToken = new RefreshToken(
                tokenValue,
                user,
                LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000));

        refreshTokenRepository.save(refreshToken);
    }

    private Role getRoleByUserType(ApplicationUserType userType) {
        return switch (userType) {
            case CLIENT -> roleRepository.findByName("CLIENT")
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle CLIENT non trouvé"));
            case BARBER -> roleRepository.findByName("BARBER")
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle BARBER non trouvé"));
            case SALON_OWNER -> roleRepository.findByName("SALON_OWNER")
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle SALON_OWNER non trouvé"));
            case ADMIN -> roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle ADMIN non trouvé"));
        };
    }

    private ApplicationUserDto convertToUserDTO(ApplicationUser user) {
        ApplicationUserDto userDTO = new ApplicationUserDto();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setPhone(user.getPhone());
        userDTO.setIsEnabled(user.isEnabled());
        userDTO.setIsVerified(user.getIsVerified());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setLastLogin(user.getLastLogin());

        // Mapper les rôles
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        userDTO.setRoles(roles);

        // Mapper les permissions
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
        userDTO.setPermissions(permissions);

        return userDTO;
    }

}
