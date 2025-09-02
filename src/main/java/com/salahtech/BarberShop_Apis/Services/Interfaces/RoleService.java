package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.RoleDto;
import org.springframework.data.domain.*;

public interface RoleService {
    Page<RoleDto> search(String q, Pageable pageable);

    RoleDto create(RoleDto dto);

    RoleDto update(Long id, RoleDto dto);

    void delete(Long id);

    RoleDto findById(Long id);
}
