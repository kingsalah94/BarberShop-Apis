package com.salahtech.BarberShop_Apis.Dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Permission;
import com.salahtech.BarberShop_Apis.models.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data Transfer Object for ApplicationUser.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationUserDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profileImageUrl;
    private String preferences;
    private Boolean isEnabled;
    private Boolean isVerified;
    private String verificationToken;
    private String resetPasswordToken;
    private LocalDateTime resetPasswordExpires;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    private AuthProvider provider;
    private String providerId;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private Set<String> roles;
    private Set<String> permissions;
    private ApplicationUserType userType;
    
    // Relations
    private BarberDto barberDto;
    
    @JsonIgnore
    private List<BookingDto> bookings;
    
    // Champs calculés
    private String fullName;

    /**
     * Convertit une entité ApplicationUser en DTO
     */
    public static ApplicationUserDto fromEntity(ApplicationUser user) {
        if (user == null) {
            return null;
        }
        
        return ApplicationUserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .userType(user.getUserType())
                .profileImageUrl(user.getProfileImageUrl())
                .preferences(user.getPreferences())
                .isEnabled(user.getIsEnabled())
                .isVerified(user.getIsVerified())
                .verificationToken(user.getVerificationToken())
                .resetPasswordToken(user.getResetPasswordToken())
                .resetPasswordExpires(user.getResetPasswordExpires())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .accountNonExpired(user.isAccountNonExpired())
                .accountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .barberDto(user.getBarber() != null ? BarberDto.fromEntity(user.getBarber()) : null)
                .roles(user.getRoles() != null ? 
                       user.getRoles().stream()
                           .map(Role::getName)
                           .collect(Collectors.toSet()) : null)
                .permissions(user.getRoles() != null ? 
                            user.getRoles().stream()
                                .flatMap(role -> role.getPermissions().stream())
                                .map(Permission::getName)
                                .collect(Collectors.toSet()) : null)
                .bookings(user.getBookings() != null ?
                         user.getBookings().stream()
                             .map(BookingDto::fromEntity)
                             .collect(Collectors.toList()) : null)
                .fullName(user.getFullName())
                .build();
    }

    /**
     * Convertit un DTO en entité ApplicationUser
     * Note: Ne convertit pas le mot de passe et les relations complexes
     */
    public static ApplicationUser toEntity(ApplicationUserDto dto) {
        if (dto == null) {
            return null;
        }
        
        ApplicationUser user = new ApplicationUser();
        user.setId(dto.getId());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setUserType(dto.getUserType());
        user.setProfileImageUrl(dto.getProfileImageUrl());
        user.setPreferences(dto.getPreferences());
        user.setIsEnabled(dto.getIsEnabled());
        user.setIsVerified(dto.getIsVerified());
        user.setVerificationToken(dto.getVerificationToken());
        user.setResetPasswordToken(dto.getResetPasswordToken());
        user.setResetPasswordExpires(dto.getResetPasswordExpires());
        user.setCreatedAt(dto.getCreatedAt());
        user.setUpdatedAt(dto.getUpdatedAt());
        user.setLastLogin(dto.getLastLogin());
        user.setProvider(dto.getProvider());
        user.setProviderId(dto.getProviderId());
        user.setAccountNonExpired(dto.isAccountNonExpired());
        user.setAccountNonLocked(dto.isAccountNonLocked());
        user.setCredentialsNonExpired(dto.isCredentialsNonExpired());
        
        // Conversion du barber si présent
        if (dto.getBarberDto() != null) {
            user.setBarber(BarberDto.toEntity(dto.getBarberDto()));
        }
        
        // Note: La conversion des rôles et permissions de String vers les entités
        // doit être gérée par un service avec accès aux repositories
        // user.setRoles(convertRoles(dto.getRoles()));
        // user.setBookings(convertBookings(dto.getBookings()));
        
        return user;
    }

    /**
     * Crée un DTO simplifié pour les réponses publiques (sans informations sensibles)
     */
    public static ApplicationUserDto createPublicDto(ApplicationUser user) {
        if (user == null) {
            return null;
        }
        
        return ApplicationUserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail()) // Vous pourriez vouloir masquer ceci aussi
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .barberDto(user.getBarber() != null ? BarberDto.fromEntity(user.getBarber()) : null)
                .fullName(user.getFullName())
                .build();
    }

    /**
     * Crée un DTO pour l'authentification (avec rôles et permissions)
     */
    public static ApplicationUserDto createAuthDto(ApplicationUser user) {
        if (user == null) {
            return null;
        }
        
        return ApplicationUserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .preferences(user.getPreferences())
                .isEnabled(user.getIsEnabled())
                .isVerified(user.getIsVerified())
                .lastLogin(user.getLastLogin())
                .provider(user.getProvider())
                .accountNonExpired(user.isAccountNonExpired())
                .accountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .roles(user.getRoles() != null ? 
                       user.getRoles().stream()
                           .map(Role::getName)
                           .collect(Collectors.toSet()) : null)
                .permissions(user.getRoles() != null ? 
                            user.getRoles().stream()
                                .flatMap(role -> role.getPermissions().stream())
                                .map(Permission::getName)
                                .collect(Collectors.toSet()) : null)
                .fullName(user.getFullName())
                .build();
    }

    /**
     * Met à jour une entité existante avec les données du DTO
     */
    public static void updateEntity(ApplicationUser user, ApplicationUserDto dto) {
        if (user == null || dto == null) {
            return;
        }
        
        // Ne met à jour que les champs modifiables
        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getProfileImageUrl() != null) {
            user.setProfileImageUrl(dto.getProfileImageUrl());
        }
        if (dto.getPreferences() != null) {
            user.setPreferences(dto.getPreferences());
        }
        if (dto.getIsEnabled() != null) {
            user.setIsEnabled(dto.getIsEnabled());
        }
        if (dto.getIsVerified() != null) {
            user.setIsVerified(dto.getIsVerified());
        }
        
        // Met à jour automatiquement la date de modification
        user.setUpdatedAt(LocalDateTime.now());
    }

    // Méthodes utilitaires
    public boolean hasRole(String roleName) {
        return roles != null && roles.contains(roleName);
    }
    
    public boolean hasPermission(String permissionName) {
        return permissions != null && permissions.contains(permissionName);
    }
    
    public boolean isActive() {
        return Boolean.TRUE.equals(isEnabled) && Boolean.TRUE.equals(isVerified);
    }
}