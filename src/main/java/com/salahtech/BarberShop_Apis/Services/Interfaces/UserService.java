package com.salahtech.BarberShop_Apis.Services.Interfaces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.*;
import com.salahtech.BarberShop_Apis.Dtos.AuthStatsDTO;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Exceptions.UserAlreadyExistsException;
import com.salahtech.BarberShop_Apis.Services.EmailService;
import com.salahtech.BarberShop_Apis.Utils.JwtUtil;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Auth.AuthResponse;
import com.salahtech.BarberShop_Apis.models.Auth.LoginRequest;
import com.salahtech.BarberShop_Apis.models.Auth.RegisterRequest;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * Authentification d'un utilisateur
     */
    public AuthResponse login(LoginRequest loginRequest) throws AuthenticationException {
        try {
            logger.info("Tentative de connexion pour l'utilisateur: {}", loginRequest.getEmail());
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail().toLowerCase().trim(),
                            loginRequest.getPassword()
                    )
            );

            ApplicationUser user = (ApplicationUser) authentication.getPrincipal();
            
            // Vérifier si le compte est actif
            if (!user.isEnabled()) {
                throw new BadCredentialsException("Compte désactivé");
            }

            // Générer les tokens
            String accessToken = jwtUtil.generateToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);

            // Mettre à jour la dernière connexion
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            logger.info("Connexion réussie pour l'utilisateur: {}", user.getEmail());

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole().name()
            );

        } catch (BadCredentialsException e) {
            logger.warn("Échec de connexion pour l'utilisateur: {} - Identifiants invalides", loginRequest.getEmail());
            throw new BadCredentialsException("Identifiants invalides");
        }
    }

    /**
     * Inscription d'un nouvel utilisateur
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        String email = registerRequest.getEmail().toLowerCase().trim();
        
        logger.info("Tentative d'inscription pour l'utilisateur: {}", email);

        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(email)) {
            logger.warn("Tentative d'inscription avec un email déjà existant: {}", email);
            throw new UserAlreadyExistsException("Un compte existe déjà avec cet email");
        }

        // Créer un nouvel utilisateur
        ApplicationUser user = new ApplicationUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName().trim());
        user.setLastName(registerRequest.getLastName().trim());
        user.setRole(ApplicationUser.Role.CLIENT); // Par défaut, nouvel utilisateur = client
        
        if (registerRequest.getPhoneNumber() != null && !registerRequest.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(registerRequest.getPhoneNumber().trim());
        }

        ApplicationUser savedUser = userRepository.save(user);

        // Générer les tokens
        String accessToken = jwtUtil.generateToken(savedUser);
        String refreshToken = jwtUtil.generateRefreshToken(savedUser);

        logger.info("Inscription réussie pour l'utilisateur: {}", savedUser.getEmail());

        // Envoyer email de bienvenue (asynchrone)
        try {
            emailService.sendWelcomeEmail(savedUser);
        } catch (Exception e) {
            logger.warn("Erreur lors de l'envoi de l'email de bienvenue à {}: {}", savedUser.getEmail(), e.getMessage());
        }

        return new AuthResponse(
                accessToken,
                refreshToken,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole().name()
        );
    }

    /**
     * Rafraîchir le token d'accès
     */
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // Vérifier si c'est bien un refresh token
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new BadCredentialsException("Token de rafraîchissement invalide");
            }

            // Vérifier si le token n'est pas expiré
            if (jwtUtil.isTokenExpired(refreshToken)) {
                throw new BadCredentialsException("Token de rafraîchissement expiré");
            }

            // Extraire l'utilisateur
            String email = jwtUtil.extractUsername(refreshToken);
            ApplicationUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

            // Générer un nouveau token d'accès
            String newAccessToken = jwtUtil.generateToken(user);

            logger.info("Token rafraîchi avec succès pour l'utilisateur: {}", user.getEmail());

            return new AuthResponse(
                    newAccessToken,
                    refreshToken, // Garder le même refresh token
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole().name()
            );

        } catch (Exception e) {
            logger.warn("Échec du rafraîchissement de token: {}", e.getMessage());
            throw new BadCredentialsException("Token de rafraîchissement invalide");
        }
    }

    /**
     * Récupérer les informations de l'utilisateur connecté
     */
    @Transactional(readOnly = true)
    public UserDTO getCurrentUserInfo(String email) {
        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        return UserDTO.fromUser(user);
    }

    /**
     * Charger un utilisateur à partir d'un token
     */
    public UserDetails loadUserByToken(String token) {
        String email = jwtUtil.extractUsername(token);
        return userDetailsService.loadUserByUsername(email);
    }

    /**
     * Envoyer un email de réinitialisation de mot de passe
     */
    public void sendPasswordResetEmail(String email) {
        ApplicationUser user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte trouvé avec cet email"));

        // Générer un token de réinitialisation
        String resetToken = generatePasswordResetToken(user);
        
        // Sauvegarder le token (vous pouvez créer une entité PasswordResetToken)
        // Pour simplifier, on utilise un champ temporaire dans User
        // En production, créez une table séparée pour les tokens de reset
        
        logger.info("Email de réinitialisation envoyé à: {}", email);
        
        try {
            emailService.sendPasswordResetEmail(user, resetToken);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de réinitialisation à {}: {}", email, e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }

    /**
     * Réinitialiser le mot de passe avec un token
     */
    public void resetPassword(String token, String newPassword) {
        // Valider le token de réinitialisation
        // Dans un vrai projet, vous valideriez contre une table PasswordResetToken
        
        try {
            String email = jwtUtil.extractUsername(token);
            ApplicationUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

            // Encoder et sauvegarder le nouveau mot de passe
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setCredentialsNonExpired(true); // Réactiver les credentials
            userRepository.save(user);

            logger.info("Mot de passe réinitialisé avec succès pour l'utilisateur: {}", email);

            // Envoyer confirmation par email
            try {
                emailService.sendPasswordChangedConfirmation(user);
            } catch (Exception e) {
                logger.warn("Erreur lors de l'envoi de l'email de confirmation à {}: {}", email, e.getMessage());
            }

        } catch (Exception e) {
            logger.warn("Échec de la réinitialisation de mot de passe: {}", e.getMessage());
            throw new BadCredentialsException("Token de réinitialisation invalide ou expiré");
        }
    }

    /**
     * Changer le mot de passe d'un utilisateur connecté
     */
    public void changePassword(String email, String currentPassword, String newPassword) {
        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Vérifier le mot de passe actuel
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Mot de passe actuel incorrect");
        }

        // Encoder et sauvegarder le nouveau mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logger.info("Mot de passe changé avec succès pour l'utilisateur: {}", email);
    }

    /**
     * Désactiver un compte utilisateur
     */
    public void deactivateAccount(String email) {
        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        user.disableAccount();
        userRepository.save(user);

        logger.info("Compte désactivé pour l'utilisateur: {}", email);
    }

    /**
     * Réactiver un compte utilisateur
     */
    public void reactivateAccount(String email) {
        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        user.enableAccount();
        userRepository.save(user);

        logger.info("Compte réactivé pour l'utilisateur: {}", email);
    }

    /**
     * Générer un token de réinitialisation de mot de passe
     */
    private String generatePasswordResetToken(ApplicationUser user) {
        // Créer un token temporaire avec une durée de vie courte (30 minutes)
        return UUID.randomUUID().toString() + "_" + user.getId() + "_" + System.currentTimeMillis();
    }

    /**
     * Vérifier si un email est disponible
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email.toLowerCase().trim());
    }

    /**
     * Obtenir les statistiques d'authentification
     */
    @Transactional(readOnly = true)
    public AuthStatsDTO getAuthStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAllActiveUsers().size();
        long clientsCount = userRepository.findByRole(ApplicationUser.Role.CLIENT).size();
        long barbersCount = userRepository.findByRole(ApplicationUser.Role.BARBER).size();

        return new AuthStatsDTO(totalUsers, activeUsers, clientsCount, barbersCount);
    }
}