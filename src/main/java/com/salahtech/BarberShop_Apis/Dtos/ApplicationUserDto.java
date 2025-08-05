package com.salahtech.BarberShop_Apis.Dtos;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Permission;
import com.salahtech.BarberShop_Apis.models.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


/**
 * Data Transfer Object for User.
 */
@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ApplicationUserDto {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String profileImageUrl;

    private String preferences;

    private AuthProvider role;

    private BarberDto barberDto;

    private boolean isAccountNonExpired;

    private boolean isAccountNonLocked;

    private boolean credentialsNonExpired;

    private Boolean isEnabled;

    private Boolean isVerified;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastLogin;

    private Set<Role> roles;
    private Set<Permission> permissions;

    @JsonIgnore
    private List<BookingDto> bookings;



    public static ApplicationUserDto fromEntity(ApplicationUser user) {
        return ApplicationUserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                //.barberDto(BarberDto.fromEntity(user.getBarber()))
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .preferences(user.getPreferences())
                .roles(user.getRoles())
                .isAccountNonExpired(user.isAccountNonExpired())
                .isAccountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .isEnabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static ApplicationUser toEntity(ApplicationUserDto dto) {
        if (dto == null) {
          return null;
        }
        ApplicationUser user = new ApplicationUser();
        user.setId(dto.id);
        user.setFirstName(dto.firstName);
        user.setLastName(dto.lastName);
        user.setEmail(dto.email);
        user.setBarber(BarberDto.toEntity(dto.getBarberDto()));
        user.setPhone(dto.phone);
        user.setProfileImageUrl(dto.profileImageUrl);
        user.setPreferences(dto.preferences);
        user.setRoles(dto.getRoles());
        user.setAccountNonExpired(dto.isAccountNonExpired());
        user.setAccountNonLocked(dto.isAccountNonLocked);
        user.setCredentialsNonExpired(dto.credentialsNonExpired);
        user.setIsEnabled(dto.isEnabled);
        user.setCreatedAt(dto.createdAt);
        user.setUpdatedAt(dto.updatedAt);
        return user;
    }

    
}
