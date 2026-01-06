package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Permission;
import com.salahtech.BarberShop_Apis.models.Role;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationUserDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email; // garde si besoin, sinon masque côté public
    private String phone;
    private String profileImageUrl;
    private String preferences;
    private Boolean isEnabled;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    private AuthProvider provider;
    private boolean mustChangePassword;
    private String providerId;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private Set<String> roles;
    private Set<String> permissions;
    private ApplicationUserType userType;

    private BarberDto barberDto; // seulement en lecture

    private String fullName;

    public static ApplicationUserDto fromEntity(ApplicationUser user) {
        if (user == null)
            return null;

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
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .mustChangePassword(user.isMustChangePassword())
                .accountNonExpired(user.isAccountNonExpired())
                .accountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .barberDto(user.getBarber() != null ? BarberDto.fromEntity(user.getBarber()) : null)
                .roles(user.getRoles() != null ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                        : null)
                .permissions(
                        user.getRoles() != null
                                ? user.getRoles().stream().flatMap(r -> r.getPermissions().stream())
                                        .map(Permission::getName).collect(Collectors.toSet())
                                : null)
                .fullName(user.getFullName())
                .build();
    }

    /** DTO -> Entity (champ simples seulement, pas les relations ni les dates). */
    public static ApplicationUser toEntity(ApplicationUserDto dto) {
        if (dto == null)
            return null;
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
        user.setProvider(dto.getProvider());
        user.setProviderId(dto.getProviderId());
        user.setMustChangePassword(dto.isMustChangePassword());
        user.setAccountNonExpired(dto.isAccountNonExpired());
        user.setAccountNonLocked(dto.isAccountNonLocked());
        user.setCredentialsNonExpired(dto.isCredentialsNonExpired());
        // createdAt/updatedAt/lastLogin gérés par l’entité ou le service
        // roles/permissions gérés par service (repositories)
        return user;
    }

    /** Variante plus “publique” (masque phone/email si tu veux) */
    public static ApplicationUserDto createPublicDto(ApplicationUser user) {
        if (user == null)
            return null;
        return ApplicationUserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail()) // masque si nécessaire
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .barberDto(user.getBarber() != null ? BarberDto.fromEntity(user.getBarber()) : null)
                .fullName(user.getFullName())
                .build();
    }

    public static ApplicationUserDto createAuthDto(ApplicationUser user) {
        return fromEntity(user);
    }

    public static void updateEntity(ApplicationUser user, ApplicationUserDto dto) {
        if (user == null || dto == null)
            return;
        if (dto.getFirstName() != null)
            user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null)
            user.setLastName(dto.getLastName());
        if (dto.getPhone() != null)
            user.setPhone(dto.getPhone());
        if (dto.getProfileImageUrl() != null)
            user.setProfileImageUrl(dto.getProfileImageUrl());
        if (dto.getPreferences() != null)
            user.setPreferences(dto.getPreferences());
        if (dto.getIsEnabled() != null)
            user.setIsEnabled(dto.getIsEnabled());
        if (dto.getIsVerified() != null)
            user.setIsVerified(dto.getIsVerified());
        user.setUpdatedAt(LocalDateTime.now());
    }
}
