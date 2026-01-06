package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.*;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.Exceptions.BadRequestException;
import com.salahtech.BarberShop_Apis.Exceptions.InvalidCredentialsException;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.EmailService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ApplicationUserService;
import com.salahtech.BarberShop_Apis.Utils.JwtUtil;
import com.salahtech.BarberShop_Apis.models.*;
import com.salahtech.BarberShop_Apis.reppsitories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApplicationUserServiceImpl implements ApplicationUserService {

    private final ApplicationUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ClientRepository clientRepository;
    private final BarberRepository barberRepository;
    private final EmailService emailService;

    private static final String TEMP_PROFILE_IMAGE_BASE_URL = "/user/image/profile/temp/";

    // ========================================================================
    // 🔐 AUTHENTIFICATION (LOGIN / REGISTER)
    // ========================================================================

    @Override
    public AuthResponseDTO login(AuthRequestDTO authRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword()));

        UserDetails principal = (UserDetails) auth.getPrincipal();
        ApplicationUser user = getUserByEmailOrThrow(principal.getUsername());

        validateUserStatus(user);

        // Génération Tokens
        String accessToken = jwtUtil.generateAccessToken(principal);
        String refreshToken = jwtUtil.generateRefreshToken(principal);

        // ✅ FIX: Supprimer l'ancien token AVANT de sauvegarder le nouveau
        saveRefreshToken(user, refreshToken);

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                convertToUserDTO(user),
                user.isMustChangePassword());
    }

    @Override
    public AuthResponseDTO register(RegisterRequestDTO req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Un compte avec cet email existe déjà");
        }

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
        u.setProfileImageUrl(getTemporaryProfileImageUrl(req.getFirstName()));

        u = userRepository.save(u);

        createBusinessProfile(u);

        try {
            emailService.sendVerificationEmail(u);
        } catch (Exception e) {
            log.warn("Erreur envoi email verif: {}", e.getMessage());
        }

        // Auto-login après register
        String accessToken = jwtUtil.generateAccessToken(u);
        String refreshToken = jwtUtil.generateRefreshToken(u);
        saveRefreshToken(u, refreshToken);

        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                convertToUserDTO(u),
                false);
    }

    @Override
    public AuthResponseDTO refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BadRequestException("Refresh token invalide ou expiré");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        ApplicationUser u = getUserByEmailOrThrow(username);

        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Refresh token non trouvé en base"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new BadRequestException("Refresh token expiré");
        }

        // ✅ Rotation du token: supprimer l'ancien
        refreshTokenRepository.delete(stored);

        String newAccess = jwtUtil.generateAccessToken(u);
        String newRefresh = jwtUtil.generateRefreshToken(u);
        saveRefreshToken(u, newRefresh);

        return new AuthResponseDTO(
                newAccess,
                newRefresh,
                jwtUtil.getAccessTokenExpiration(),
                convertToUserDTO(u),
                u.isMustChangePassword());
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(refreshTokenRepository::delete);
        }
    }

    // ========================================================================
    // 👤 GESTION PROFIL (UPDATE, AVATAR, PASSWORD)
    // ========================================================================

    @Override
    public ApplicationUserDto updateMe(String email, UpdateProfileRequest req) {
        ApplicationUser user = getUserByEmailOrThrow(email);

        if (req.getFirstName() != null)
            user.setFirstName(req.getFirstName());
        if (req.getLastName() != null)
            user.setLastName(req.getLastName());
        if (req.getPhone() != null)
            user.setPhone(req.getPhone());
        if (req.getPreferences() != null)
            user.setPreferences(req.getPreferences());

        // ✅ MISE A JOUR AVEC SYNCHRONISATION
        if (req.getProfileImageUrl() != null) {
            updateProfileImageAcrossEntities(user, req.getProfileImageUrl());
        }

        user = userRepository.save(user);
        return convertToUserDTO(user);
    }

    @Override
    public ApplicationUserDto updateAvatar(Long userId, String fileUrl) {
        ApplicationUser user = getUserByIdOrThrow(userId);

        // ✅ MISE A JOUR AVEC SYNCHRONISATION
        updateProfileImageAcrossEntities(user, fileUrl);

        ApplicationUser saved = userRepository.save(user);
        return convertToUserDTO(saved);
    }

    /**
     * 🛠️ Helper pour synchroniser l'image entre User, Client et Barber
     */
    private void updateProfileImageAcrossEntities(ApplicationUser user, String imageUrl) {
        // 1. Update User
        user.setProfileImageUrl(imageUrl);

        // 2. Update Client (si existe)
        if (user.getClient() != null) {
            user.getClient().setProfileImageUrl(imageUrl);
            // clientRepository.save(user.getClient()); // Optionnel grâce au Cascade, mais
            // plus sûr
        }

        // 3. Update Barber (si existe)
        if (user.getBarber() != null) {
            user.getBarber().setProfileImageUrl(imageUrl);
            // barberRepository.save(user.getBarber()); // Optionnel grâce au Cascade
        }
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        ApplicationUser user = getUserByEmailOrThrow(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("L'ancien mot de passe est incorrect.");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new InvalideEntityException("Les nouveaux mots de passe ne correspondent pas.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // ✅ Sécurité : déconnexion de tous les appareils
        refreshTokenRepository.deleteByUser(user);
    }

    @Override
    public void forgotPassword(String email) {
        ApplicationUser u = getUserByEmailOrThrow(email);

        String resetToken = UUID.randomUUID().toString();
        u.setResetPasswordToken(resetToken);
        u.setResetPasswordExpires(LocalDateTime.now().plusHours(1));
        userRepository.save(u);

        try {
            emailService.sendPasswordResetEmail(u, resetToken);
        } catch (Exception e) {
            log.warn("Erreur envoi email reset: {}", e.getMessage());
        }
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        ApplicationUser u = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new BadRequestException("Token invalide"));

        if (u.getResetPasswordExpires().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token expiré");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        u.setResetPasswordToken(null);
        u.setResetPasswordExpires(null);
        u.setMustChangePassword(false);
        userRepository.save(u);

        // ✅ Force reconnexion
        refreshTokenRepository.deleteByUser(u);
    }

    @Override
    public void saveFcmToken(String email, String token) {
        ApplicationUser user = getUserByEmailOrThrow(email);
        user.setFcmToken(token);
        userRepository.save(user);
    }

    @Override
    public void verifyEmail(String token) {
        ApplicationUser u = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Token invalide"));

        u.setIsVerified(true);
        u.setVerificationToken(null);
        userRepository.save(u);

        try {
            emailService.sendWelcomeEmail(u);
        } catch (Exception e) {
            log.warn("Erreur envoi email welcome: {}", e.getMessage());
        }
    }

    // ========================================================================
    // ⚙️ MÉTHODES SYSTÈME & ADMIN
    // ========================================================================

    @Override
    public ApplicationUser registerSystemAdmin(ApplicationUser user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return userRepository.findByEmail(user.getEmail()).orElseThrow();
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setIsEnabled(true);
        user.setIsVerified(true);
        return userRepository.save(user);
    }

    @Override
    public ApplicationUser findById(Long id) {
        return getUserByIdOrThrow(id);
    }

    @Override
    public ApplicationUser findByEmail(String email) {
        return getUserByEmailOrThrow(email);
    }

    @Override
    public ApplicationUser findByEmailOrCreate(String email, String name, AuthProvider provider, String providerId) {
        Optional<ApplicationUser> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            ApplicationUser u = existing.get();
            u.setLastLogin(LocalDateTime.now());
            return userRepository.save(u);
        }

        ApplicationUser u = new ApplicationUser();
        u.setEmail(email);
        String[] parts = name != null ? name.split(" ", 2) : new String[] { "", "" };
        u.setFirstName(parts[0]);
        u.setLastName(parts.length > 1 ? parts[1] : "");
        u.setProvider(provider);
        u.setProviderId(providerId);
        u.setIsEnabled(true);
        u.setIsVerified(true);
        u.setUserType(ApplicationUserType.CLIENT);
        u.setRoles(Set.of(getRoleByUserType(ApplicationUserType.CLIENT)));

        u = userRepository.save(u);
        createClientProfile(u);

        return u;
    }

    // ========================================================================
    // 🛠️ HELPER METHODS (PRIVATE)
    // ========================================================================

    private ApplicationUser getUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé : " + email));
    }

    private ApplicationUser getUserByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé ID : " + id));
    }

    private void validateUserStatus(ApplicationUser user) {
        if (!user.isEnabled()) {
            throw new BadRequestException("Compte désactivé.");
        }
    }

    /**
     * ✅ FIX: Supprime l'ancien refresh token avant d'en créer un nouveau
     * Cela évite l'erreur: duplicate key value violates unique constraint
     * "refresh_tokens_user_id_key"
     */
    // private void saveRefreshToken(ApplicationUser user, String tokenValue) {
    // // Supprimer l'ancien token s'il existe
    // refreshTokenRepository.findByUser(user).ifPresent(oldToken -> {
    // log.debug("Suppression de l'ancien refresh token pour user_id={}",
    // user.getId());
    // refreshTokenRepository.delete(oldToken);
    // });

    // // Créer et sauvegarder le nouveau token
    // RefreshToken rt = new RefreshToken(
    // tokenValue,
    // user,
    // LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000));
    // refreshTokenRepository.save(rt);

    // log.debug("Nouveau refresh token créé pour user_id={}", user.getId());
    // }
    private void saveRefreshToken(ApplicationUser user, String tokenValue) {
        // 1. Chercher si un token existe déjà pour cet utilisateur
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElseGet(RefreshToken::new); // Si non, on crée une instance vide

        // 2. Mettre à jour les données (que ce soit un nouveau ou un ancien)
        refreshToken.setToken(tokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000));

        // 3. Sauvegarder (JPA fera un UPDATE si l'ID existe, sinon un INSERT)
        refreshTokenRepository.save(refreshToken);

        log.debug("Refresh token mis à jour pour user_id={}", user.getId());
    }

    private Role getRoleByUserType(ApplicationUserType userType) {
        String roleName = switch (userType) {
            case CLIENT -> "CLIENT";
            case BARBER -> "BARBER";
            case SALON_OWNER -> "SALON_OWNER";
            case ADMIN -> "ADMIN";
        };
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle " + roleName + " non trouvé en base"));
    }

    private void createBusinessProfile(ApplicationUser user) {
        if (user.getUserType() == ApplicationUserType.CLIENT) {
            createClientProfile(user);
        } else if (user.getUserType() == ApplicationUserType.BARBER
                || user.getUserType() == ApplicationUserType.SALON_OWNER) {
            createBarberProfile(user);
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
                    // ✅ CORRECTION : On prend l'image du USER (pas une nouvelle génération)
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();
            clientRepository.save(client);
            user.setClient(client);
        }
    }

    private void createBarberProfile(ApplicationUser user) {
        if (!barberRepository.existsByUser_Id(user.getId())) {
            Barber barber = Barber.builder()
                    .user(user)
                    .name(user.getFullName())
                    .salonName("À configurer")
                    .available(false)
                    .priceFrom(BigDecimal.ZERO)
                    // ✅ CORRECTION : On prend l'image du USER
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();
            barberRepository.save(barber);
            user.setBarber(barber);
        }
    }

    private ApplicationUserDto convertToUserDTO(ApplicationUser user) {
        return ApplicationUserDto.fromEntity(user);
    }

    private String getTemporaryProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(TEMP_PROFILE_IMAGE_BASE_URL + (username != null ? username : "default"))
                .toUriString();
    }
}