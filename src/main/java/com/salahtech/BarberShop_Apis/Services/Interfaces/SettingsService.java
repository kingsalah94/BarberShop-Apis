package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.SystemStatusDto;
import com.salahtech.BarberShop_Apis.models.GlobalSettings;

public interface SettingsService {

    GlobalSettings getSettings();

    GlobalSettings updateSettings(GlobalSettings newSettings);

    SystemStatusDto getSystemStatus();

    void clearCache();

}
