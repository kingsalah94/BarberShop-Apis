package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.DeviceTokenDto;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.Set;

public interface DeviceTokenService {
    Page<DeviceTokenDto> search(Long userId, Boolean active, Set<String> types, LocalDateTime updatedBefore,
            Pageable pageable);

    DeviceTokenDto register(DeviceTokenDto dto); // upsert par token

    DeviceTokenDto activate(Long id, boolean value);

    void delete(Long id);

    DeviceTokenDto findById(Long id);
}
