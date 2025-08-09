package com.salahtech.BarberShop_Apis.Services.Implementations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Dtos.AuthRequestDTO;
import com.salahtech.BarberShop_Apis.Dtos.AuthResponseDTO;
import com.salahtech.BarberShop_Apis.Dtos.RegisterRequestDTO;
import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.EmailService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ApplicationUserService;
import com.salahtech.BarberShop_Apis.Utils.JwtUtil;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.models.RefreshToken;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.RefreshTokenRepository;
import com.salahtech.BarberShop_Apis.reppsitories.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import com.salahtech.BarberShop_Apis.Exceptions.BadRequestException;
import com.salahtech.BarberShop_Apis.models.Permission;
import java.util.UUID;

@Service
@AllArgsConstructor
@NoArgsConstructor
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
    private EmailService emailService;
    
    @Override
    public ApplicationUser findByEmailOrCreate(String email, String name, AuthProvider provider, String providerId) {
        Optional<ApplicationUser> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            ApplicationUser user = existingUser.get();
            user.setLastLogin(LocalDateTime.now());
            return userRepository.save(user);
        }
        
        // Créer un nouvel utilisateur OAuth2
        ApplicationUser newUser = new ApplicationUser();
        newUser.setEmail(email);
        
        // Diviser le nom complet
        String[] nameParts = name.split(" ", 2);
        newUser.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            newUser.setLastName(nameParts[1]);
        } else {
            newUser.setLastName("");
        }
        
        newUser.setProvider(provider);
        newUser.setProviderId(providerId);
        newUser.setIsEnabled(true);
        newUser.setIsVerified(true); // Les comptes OAuth2 sont automatiquement vérifiés
        newUser.setPassword(""); // Pas de mot de passe pour OAuth2
        
        // Assigner le rôle CLIENT par défaut
        Role clientRole = roleRepository.findByName("CLIENT")
                .orElseThrow(() -> new ResourceNotFoundException("Rôle CLIENT non trouvé"));
        newUser.setRoles(Set.of(clientRole));
        
        return userRepository.save(newUser);
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
        // Authentifier l'utilisateur
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getEmail(),
                        authRequest.getPassword()
                )
        );
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        ApplicationUser user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        // Vérifier que l'utilisateur est activé et vérifié
        if (!user.isEnabled()) {
            throw new BadRequestException("Compte désactivé. Contactez l'administrateur.");
        }
        
        if (!user.getIsVerified()) {
            throw new BadRequestException("Email non vérifié. Vérifiez votre boîte mail.");
        }
        
        // Générer les tokens
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        
        // Sauvegarder le refresh token
        saveRefreshToken(user, refreshToken);
        
        // Mettre à jour la dernière connexion
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        // Préparer la réponse
        ApplicationUserDto userDTO = convertToUserDTO(user);
        
        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                userDTO
        );
    }
    
    @Override
    public AuthResponseDTO register(RegisterRequestDTO registerRequest) {
        // Vérifier si l'utilisateur existe déjà
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Un compte avec cet email existe déjà");
        }
        
        // Créer le nouvel utilisateur
        ApplicationUser user = new ApplicationUser();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhone(registerRequest.getPhone());
        user.setIsEnabled(true);
        user.setIsVerified(false);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setProvider(AuthProvider.LOCAL);
        
        // Assigner le rôle approprié
        Role role = getRoleByUserType(registerRequest.getUserType());
        user.setRoles(Set.of(role));
        
        // Sauvegarder l'utilisateur
        user = userRepository.save(user);
        
        // Envoyer l'email de vérification
        emailService.sendVerificationEmail(user);
        
        // Générer les tokens (l'utilisateur peut se connecter mais doit vérifier son email)
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        // Sauvegarder le refresh token
        saveRefreshToken(user, refreshToken);
        
        // Préparer la réponse
        ApplicationUserDto userDTO = convertToUserDTO(user);
        
        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpiration(),
                userDTO
        );
    }
    
    @Override
    public AuthResponseDTO refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BadRequestException("Refresh token invalide ou expiré");
        }
        
        String username = jwtUtil.extractUsername(refreshToken);
        ApplicationUser user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        // Vérifier que le refresh token existe en base
        RefreshToken storedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Refresh token non trouvé"));
        
        if (storedRefreshToken.isExpired()) {
            refreshTokenRepository.delete(storedRefreshToken);
            throw new BadRequestException("Refresh token expiré");
        }
        
        // Générer un nouveau access token
        String newAccessToken = jwtUtil.generateAccessToken(user);
        
        // Optionnel : générer un nouveau refresh token (rotation)
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        
        // Supprimer l'ancien refresh token et sauvegarder le nouveau
        refreshTokenRepository.delete(storedRefreshToken);
        saveRefreshToken(user, newRefreshToken);
        
        ApplicationUserDto userDTO = convertToUserDTO(user);
        
        return new AuthResponseDTO(
                newAccessToken,
                newRefreshToken,
                jwtUtil.getAccessTokenExpiration(),
                userDTO
        );
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
        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte trouvé avec cet email"));
        
        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordExpires(LocalDateTime.now().plusHours(1)); // 1 heure
        
        userRepository.save(user);
        
        // Envoyer l'email de reset
        emailService.sendPasswordResetEmail(user, resetToken);
    }
    
    @Override
    public void resetPassword(String token, String newPassword) {
        ApplicationUser user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new BadRequestException("Token de reset invalide"));
        
        if (user.getResetPasswordExpires().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token de reset expiré");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpires(null);
        
        userRepository.save(user);
        
        // Invalider tous les refresh tokens existants
        refreshTokenRepository.deleteByUser(user);
    }
    
    @Override
    public void verifyEmail(String token) {
        ApplicationUser user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Token de vérification invalide"));
        
        user.setIsVerified(true);
        user.setVerificationToken(null);
        
        userRepository.save(user);
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
                LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000)
        );
        
        refreshTokenRepository.save(refreshToken);
    }
    
    private Role getRoleByUserType(String userType) {
        return switch (userType.toUpperCase()) {
            case "CLIENT" -> roleRepository.findByName("CLIENT")
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle CLIENT non trouvé"));
            case "BARBER" -> roleRepository.findByName("BARBER")
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle BARBER non trouvé"));
            case "SALON_OWNER" -> roleRepository.findByName("SALON_OWNER")
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle SALON_OWNER non trouvé"));
            default -> throw new BadRequestException("Type d'utilisateur invalide : " + userType);
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

