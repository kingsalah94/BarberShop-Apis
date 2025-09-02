package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.security.Dtos.SecuritySettingsDto;

public interface SecuritySettingsService {
    SecuritySettingsDto get();

    SecuritySettingsDto update(SecuritySettingsDto dto);
}
