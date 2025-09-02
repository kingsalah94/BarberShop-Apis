package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.PermissionDto;
import org.springframework.data.domain.*;

public interface PermissionService {
    Page<PermissionDto> search(String q, Pageable pageable);

    PermissionDto create(PermissionDto dto);

    PermissionDto update(Long id, PermissionDto dto);

    void delete(Long id);

    PermissionDto findById(Long id);
}
