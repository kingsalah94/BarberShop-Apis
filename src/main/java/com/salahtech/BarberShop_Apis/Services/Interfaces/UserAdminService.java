package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.Set;

public interface UserAdminService {
    Page<ApplicationUserDto> search(String q,
            Boolean enabled,
            Boolean verified,
            String role,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            Pageable pageable);

    ApplicationUserDto create(ApplicationUserDto dto, String rawPassword);

    ApplicationUserDto update(Long id, ApplicationUserDto dto);

    void delete(Long id);

    ApplicationUserDto findById(Long id);

    ApplicationUserDto setEnabled(Long id, boolean value);

    ApplicationUserDto setVerified(Long id, boolean value);

    ApplicationUserDto setRoles(Long id, Set<String> roleNames);
}
