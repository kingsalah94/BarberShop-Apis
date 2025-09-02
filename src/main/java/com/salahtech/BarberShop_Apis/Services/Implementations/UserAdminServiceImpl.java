package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Services.Interfaces.UserAdminService;
import com.salahtech.BarberShop_Apis.models.*;
import com.salahtech.BarberShop_Apis.reppsitories.*;
import com.salahtech.BarberShop_Apis.specs.UserSpecifications;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAdminServiceImpl implements UserAdminService {

    private final ApplicationUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationUserDto> search(String q, Boolean enabled, Boolean verified, String role,
            LocalDateTime createdFrom, LocalDateTime createdTo, Pageable pageable) {

        Specification<ApplicationUser> spec = Specification
                .where(UserSpecifications.emailLike(q))
                .and(UserSpecifications.nameLike(q))
                .and(UserSpecifications.enabledEq(enabled))
                .and(UserSpecifications.verifiedEq(verified))
                .and(UserSpecifications.hasRole(role))
                .and(UserSpecifications.createdBetween(createdFrom, createdTo));

        return userRepo.findAll(spec, pageable).map(ApplicationUserDto::fromEntity);
    }

    @Override
    public ApplicationUserDto create(ApplicationUserDto dto, String rawPassword) {
        ApplicationUser u = ApplicationUserDto.toEntity(dto);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setIsEnabled(Boolean.TRUE.equals(dto.getIsEnabled()));
        u.setIsVerified(Boolean.TRUE.equals(dto.getIsVerified()));
        u.setAccountNonExpired(Boolean.TRUE.equals(dto.isAccountNonExpired()));
        u.setAccountNonLocked(Boolean.TRUE.equals(dto.isAccountNonLocked()));
        // rôles par noms
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            var roles = dto.getRoles().stream()
                    .map(n -> roleRepo.findByName(n)
                            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + n)))
                    .collect(java.util.stream.Collectors.toSet());
            u.setRoles(roles);
        }
        var saved = userRepo.save(u);
        return ApplicationUserDto.fromEntity(saved);
    }

    @Override
    public ApplicationUserDto update(Long id, ApplicationUserDto dto) {
        var u = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ApplicationUserDto.updateEntity(u, dto);
        // rôles ?
        if (dto.getRoles() != null) {
            var roles = dto.getRoles().stream()
                    .map(n -> roleRepo.findByName(n)
                            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + n)))
                    .collect(java.util.stream.Collectors.toSet());
            u.setRoles(roles);
        }
        return ApplicationUserDto.fromEntity(userRepo.save(u));
    }

    @Override
    public void delete(Long id) {
        if (!userRepo.existsById(id))
            throw new ResourceNotFoundException("User not found");
        userRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationUserDto findById(Long id) {
        return userRepo.findById(id)
                .map(ApplicationUserDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public ApplicationUserDto setEnabled(Long id, boolean value) {
        var u = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        u.setIsEnabled(value);
        return ApplicationUserDto.fromEntity(userRepo.save(u));
    }

    @Override
    public ApplicationUserDto setVerified(Long id, boolean value) {
        var u = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        u.setIsVerified(value);
        return ApplicationUserDto.fromEntity(userRepo.save(u));
    }

    @Override
    public ApplicationUserDto setRoles(Long id, Set<String> roleNames) {
        var u = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var roles = roleNames.stream()
                .map(n -> roleRepo.findByName(n)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + n)))
                .collect(java.util.stream.Collectors.toSet());
        u.setRoles(roles);
        return ApplicationUserDto.fromEntity(userRepo.save(u));
    }
}
