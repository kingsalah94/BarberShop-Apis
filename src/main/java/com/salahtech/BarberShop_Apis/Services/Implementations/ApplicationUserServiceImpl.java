package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.AOP.LogActivity; // 👈 IMPORT IMPORTANT
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
import com.salahtech.BarberShop_Apis.repositories.*;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApplicationUserServiceImpl implements ApplicationUserService {

    private final ApplicationUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.salahtech.BarberShop_Apis.security.PasswordPolicy passwordPolicy;
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
    // Pas de @LogActivity ici car le LoginAuditListener s'en charge déjà
    public AuthResponseDTO login(AuthRequestDTO authRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword()));

        UserDetails principal = (UserDetails) auth.getPrincipal();
        ApplicationUser user = getUserByEmailOrThrow(principal.getUsername());

        validateUserStatus(user);

        // Expiration du mot de passe (Paramètres › Sécurité) : on impose le changement à la connexion.
        if (!user.isMustChangePassword()
                && passwordPolicy.isExpired(user.getPasswordChangedAt() != null ? user.getPasswordChangedAt() : user.getCreatedAt())) {
            user.setMustChangePassword(true);
        }

        String accessToken = jwtUtil.generateAccessToken(principal);
        String refreshToken = jwtUtil.generateRefreshToken(principal);

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
    @LogActivity(action = "REGISTER", entityName = "User") // ✅ AUDIT
    public AuthResponseDTO register(RegisterRequestDTO req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Un compte avec cet email existe déjà");
        }

        passwordPolicy.validate(req.getPassword());
        ApplicationUser u = new ApplicationUser();
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setPasswordChangedAt(LocalDateTime.now());
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setPhone(req.getPhone());
        u.setCreatedAt(LocalDateTime.now());
        u.setIsEnabled(true);
        u.setIsVerified(false);
        u.setVerificationToken(UUID.randomUUID().toString());
        u.setProvider(AuthProvider.LOCAL);
        // Inscription publique : l'utilisateur vient de choisir son mot de passe,
        // aucun changement forcé (réservé aux comptes créés par un tiers).
        u.setMustChangePassword(false);

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

        String accessToken = jwtUtil.generateAccessToken(u);
        String refreshToken = jwtUtil.generateRefreshToken(u);
        saveRefreshToken(u, refreshToken);

        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                convertToUserDTO(u),
                u.isMustChangePassword()); // false : l'inscrit vient de choisir son mot de passe
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
    @LogActivity(action = "LOGOUT", entityName = "Session") // ✅ AUDIT
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
    @LogActivity(action = "UPDATE", entityName = "User") // ✅ AUDIT
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

        if (req.getProfileImageUrl() != null) {
            updateProfileImageAcrossEntities(user, req.getProfileImageUrl());
        }

        user = userRepository.save(user);
        return convertToUserDTO(user);
    }

    @Override
    @LogActivity(action = "UPDATE_AVATAR", entityName = "User") // ✅ AUDIT
    public ApplicationUserDto updateAvatar(Long userId, String fileUrl) {
        ApplicationUser user = getUserByIdOrThrow(userId);
        updateProfileImageAcrossEntities(user, fileUrl);
        ApplicationUser saved = userRepository.save(user);
        return convertToUserDTO(saved);
    }

    private void updateProfileImageAcrossEntities(ApplicationUser user, String imageUrl) {
        user.setProfileImageUrl(imageUrl);
        if (user.getClient() != null) {
            user.getClient().setProfileImageUrl(imageUrl);
        }
        if (user.getBarber() != null) {
            user.getBarber().setProfileImageUrl(imageUrl);
        }
    }

    @Override
    @LogActivity(action = "CHANGE_PASSWORD", entityName = "User") // ✅ AUDIT
    public void changePassword(ChangePasswordRequest request) {
        ApplicationUser user = getUserByEmailOrThrow(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("L'ancien mot de passe est incorrect.");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("Les nouveaux mots de passe ne correspondent pas.");
        }

        passwordPolicy.validate(request.getNewPassword());
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Le nouveau mot de passe doit être différent de l'ancien.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);
    }

    @Override
    @LogActivity(action = "REQUEST_PASSWORD_RESET", entityName = "User") // ✅ AUDIT
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
    @LogActivity(action = "RESET_PASSWORD", entityName = "User") // ✅ AUDIT
    public void resetPassword(String token, String newPassword) {
        ApplicationUser u = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new BadRequestException("Token invalide"));

        if (u.getResetPasswordExpires().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token expiré");
        }

        passwordPolicy.validate(newPassword);
        u.setPassword(passwordEncoder.encode(newPassword));
        u.setPasswordChangedAt(LocalDateTime.now());
        u.setResetPasswordToken(null);
        u.setResetPasswordExpires(null);
        u.setMustChangePassword(false);
        userRepository.save(u);

        refreshTokenRepository.deleteByUser(u);
    }

    @Override
    public void saveFcmToken(String email, String token) {
        ApplicationUser user = getUserByEmailOrThrow(email);
        user.setFcmToken(token);
        userRepository.save(user);
    }

    @Override
    @LogActivity(action = "VERIFY_EMAIL", entityName = "User") // ✅ AUDIT
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
    @LogActivity(action = "CREATE_ADMIN", entityName = "User") // ✅ AUDIT
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
    @LogActivity(action = "SOCIAL_LOGIN", entityName = "User") // ✅ AUDIT (Creation via Google/FB)
    public ApplicationUser findByEmailOrCreate(String email, String name, AuthProvider provider, String providerId) {
        String[] parts = name != null ? name.trim().split(" ", 2) : new String[] { "" };
        return findOrCreateSocial(email, parts[0], parts.length > 1 ? parts[1] : "", provider, providerId, true);
    }

    /**
     * Connexion Google / Apple à partir d'une identité vérifiée (jeton signé par le fournisseur).
     * Compte existant (même e-mail) : connexion, à condition que le fournisseur garantisse l'e-mail.
     * Sinon : création d'un compte CLIENT (mot de passe aléatoire inutilisable, connexion par le fournisseur).
     */
    @Override
    @LogActivity(action = "SOCIAL_LOGIN", entityName = "User")
    public AuthResponseDTO socialLogin(com.salahtech.BarberShop_Apis.security.SocialTokenVerifier.SocialIdentity id,
                                       String firstName, String lastName) {
        ApplicationUser user = findOrCreateSocial(id.email(), firstName, lastName, id.provider(), id.subject(), id.emailVerified());
        validateUserStatus(user);
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        saveRefreshToken(user, refreshToken);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return new AuthResponseDTO(accessToken, refreshToken, jwtUtil.getAccessTokenExpiration(),
                // Identité prouvée par Google / Apple : pas d'écran « changer le mot de passe »
                // (l'utilisateur n'en connaît souvent aucun ; « mot de passe oublié » permet d'en définir un).
                convertToUserDTO(user), false);
    }

    private ApplicationUser findOrCreateSocial(String email, String firstName, String lastName,
                                               AuthProvider provider, String providerId, boolean emailVerified) {
        Optional<ApplicationUser> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            ApplicationUser u = existing.get();
            // Un e-mail non garanti par le fournisseur ne doit pas ouvrir un compte existant.
            if (!emailVerified) {
                throw new BadRequestException("Adresse e-mail non vérifiée par " + provider.name().charAt(0)
                        + provider.name().substring(1).toLowerCase() + " : connectez-vous avec votre mot de passe.");
            }
            if (u.getProviderId() == null && u.getProvider() != AuthProvider.LOCAL) u.setProviderId(providerId);
            u.setLastLogin(LocalDateTime.now());
            return userRepository.save(u);
        }

        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String fn = firstName != null && !firstName.isBlank() ? firstName.trim() : capitalize(local);
        String ln = lastName != null && !lastName.isBlank() ? lastName.trim() : "-";

        ApplicationUser u = new ApplicationUser();
        u.setEmail(email);
        u.setFirstName(fn);
        u.setLastName(ln);
        // Mot de passe aléatoire (jamais communiqué) : la connexion se fait par le fournisseur ;
        // « mot de passe oublié » permet d'en définir un plus tard.
        u.setPassword(passwordEncoder.encode(UUID.randomUUID() + "-" + UUID.randomUUID()));
        u.setPasswordChangedAt(LocalDateTime.now());
        u.setProvider(provider);
        u.setProviderId(providerId);
        u.setIsEnabled(true);
        u.setIsVerified(emailVerified);
        u.setMustChangePassword(false);
        u.setCreatedAt(LocalDateTime.now());
        u.setUserType(ApplicationUserType.CLIENT);
        u.setRoles(Set.of(getRoleByUserType(ApplicationUserType.CLIENT)));
        u.setProfileImageUrl(getTemporaryProfileImageUrl(fn));

        u = userRepository.save(u);
        createClientProfile(u);
        return u;
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? "Client" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ========================================================================
    // 🛠️ HELPER METHODS
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

    private void saveRefreshToken(ApplicationUser user, String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElseGet(RefreshToken::new);

        refreshToken.setToken(tokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000));

        refreshTokenRepository.save(refreshToken);
        log.debug("Refresh token mis à jour pour user_id={}", user.getId());
    }

    private Role getRoleByUserType(ApplicationUserType userType) {
        String roleName = switch (userType) {
            case CLIENT -> "CLIENT";
            case BARBER -> "BARBER";
            case SALON_OWNER -> "SALON_OWNER";
            case ADMIN -> "ADMIN";
            case MANAGER -> "MANAGER";
            case RECEPTIONIST -> "RECEPTIONIST";
            // Prestataire avec accès espace pro : rôle Spring BARBER (déjà whitelisté).
            case EMPLOYEE -> "BARBER";
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